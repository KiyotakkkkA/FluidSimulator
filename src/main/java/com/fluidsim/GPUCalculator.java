package com.fluidsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.jocl.CL;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.CL_MEM_COPY_HOST_PTR;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueue;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clSetKernelArg;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

public class GPUCalculator implements AutoCloseable {
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_kernel updateKernel;
    private cl_kernel spatialHashKernel;
    private cl_program program;
    private cl_mem persistentParticlesBuffer;
    private cl_mem neighborIndicesBuffer;
    private cl_mem neighborCountsBuffer;
    private cl_mem temperaturesBuffer;
    private cl_mem materialIndicesBuffer;
    private cl_mem materialPropertiesBuffer;
    private final int[] workDimensions;
    private final int MAX_NEIGHBORS = 64;
    private cl_kernel densityKernel;
    private cl_kernel forcesKernel;
    private cl_kernel integrationKernel;
    private cl_mem forcesBuffer;
    private cl_mem localDensitiesBuffer;
    
    private static final int WORKGROUP_SIZE = 256; // Оптимальный размер для большинства GPU

    public GPUCalculator() {
        initializeCL();
        persistentParticlesBuffer = null;
        workDimensions = new int[1];
    }

    private void initializeCL() {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_GPU;
        final int deviceIndex = 0;

        CL.setExceptionsEnabled(true);

        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        cl_platform_id platforms[] = new cl_platform_id[numPlatformsArray[0]];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        cl_device_id devices[] = new cl_device_id[numDevicesArray[0]];
        clGetDeviceIDs(platform, deviceType, numDevicesArray[0], devices, null);
        cl_device_id device = devices[deviceIndex];

        context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);

        commandQueue = clCreateCommandQueue(context, device, 0, null);

        try {
            String programSource = loadKernelSource("fluid_kernel.cl") + 
                                 loadKernelSource("spatial_hash_kernel.cl");
            program = clCreateProgramWithSource(context, 1, 
                new String[]{programSource}, null, null);
            clBuildProgram(program, 0, null, null, null, null);
            updateKernel = clCreateKernel(program, "updateParticles", null);
            spatialHashKernel = clCreateKernel(program, "buildSpatialHash", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadKernelSource(String filename) throws IOException {
        InputStream stream = getClass().getResourceAsStream("/kernels/" + filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder source = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            source.append(line).append("\n");
        }
        return source.toString();
    }

    public float[] updateParticles(float[] particles, float[] temperatures,
                                 int[] materialIndices, float[] materialProperties,
                                 int width, int height,
                                 int mouseX, int mouseY, float mouseForce,
                                 float deltaTime, float viscosity,
                                 float repulsion, float surfaceTension,
                                 float gravity, float currentMouseForce) {
        int numParticles = particles.length / 4;
        int numWorkGroups = (numParticles + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE;
        
        if (neighborIndicesBuffer == null) {
            neighborIndicesBuffer = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_int * numParticles * MAX_NEIGHBORS,
                null, null);
            
            neighborCountsBuffer = clCreateBuffer(context,
                CL_MEM_READ_WRITE,
                Sizeof.cl_int * numParticles,
                null, null);
        }

        if (persistentParticlesBuffer == null) {
            persistentParticlesBuffer = clCreateBuffer(context, 
                CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * particles.length,
                Pointer.to(particles), null);
                
            workDimensions[0] = particles.length / 4;
        }

        if (temperaturesBuffer == null || temperatures.length != numParticles) {
            if (temperaturesBuffer != null) {
                clReleaseMemObject(temperaturesBuffer);
            }
            temperaturesBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * temperatures.length, Pointer.to(temperatures), null);
        } else {
            clEnqueueWriteBuffer(commandQueue, temperaturesBuffer, CL_TRUE, 0,
                temperatures.length * Sizeof.cl_float, Pointer.to(temperatures), 0, null, null);
        }

        if (materialIndicesBuffer == null) {
            materialIndicesBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_int * materialIndices.length, Pointer.to(materialIndices), null);
            materialPropertiesBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_float * materialProperties.length, Pointer.to(materialProperties), null);
        }

        clSetKernelArg(spatialHashKernel, 0, Sizeof.cl_mem, Pointer.to(persistentParticlesBuffer));
        clSetKernelArg(spatialHashKernel, 1, Sizeof.cl_mem, Pointer.to(neighborIndicesBuffer));
        clSetKernelArg(spatialHashKernel, 2, Sizeof.cl_mem, Pointer.to(neighborCountsBuffer));
        clSetKernelArg(spatialHashKernel, 3, Sizeof.cl_int, Pointer.to(new int[]{numParticles}));
        clSetKernelArg(spatialHashKernel, 4, Sizeof.cl_float, Pointer.to(new float[]{30.0f}));
        
        clEnqueueNDRangeKernel(commandQueue, spatialHashKernel, 1, null,
                new long[]{numParticles}, null, 0, null, null);
        
        clSetKernelArg(updateKernel, 0, Sizeof.cl_mem, Pointer.to(persistentParticlesBuffer));
        clSetKernelArg(updateKernel, 1, Sizeof.cl_mem, Pointer.to(temperaturesBuffer));
        clSetKernelArg(updateKernel, 2, Sizeof.cl_mem, Pointer.to(neighborIndicesBuffer));
        clSetKernelArg(updateKernel, 3, Sizeof.cl_mem, Pointer.to(neighborCountsBuffer));
        clSetKernelArg(updateKernel, 4, Sizeof.cl_int, Pointer.to(new int[]{width}));
        clSetKernelArg(updateKernel, 5, Sizeof.cl_int, Pointer.to(new int[]{height}));
        clSetKernelArg(updateKernel, 6, Sizeof.cl_int2, Pointer.to(new int[]{mouseX, mouseY}));
        clSetKernelArg(updateKernel, 7, Sizeof.cl_float, Pointer.to(new float[]{mouseForce}));
        clSetKernelArg(updateKernel, 8, Sizeof.cl_float, Pointer.to(new float[]{deltaTime}));
        clSetKernelArg(updateKernel, 9, Sizeof.cl_float, Pointer.to(new float[]{viscosity}));
        clSetKernelArg(updateKernel, 10, Sizeof.cl_float, Pointer.to(new float[]{repulsion}));
        clSetKernelArg(updateKernel, 11, Sizeof.cl_float, Pointer.to(new float[]{surfaceTension}));
        clSetKernelArg(updateKernel, 12, Sizeof.cl_float, Pointer.to(new float[]{gravity}));
        clSetKernelArg(updateKernel, 13, Sizeof.cl_float, Pointer.to(new float[]{currentMouseForce}));
        clSetKernelArg(updateKernel, 14, Sizeof.cl_float, Pointer.to(new float[]{1.0f}));
        clSetKernelArg(updateKernel, 15, Sizeof.cl_mem, Pointer.to(materialIndicesBuffer));
        clSetKernelArg(updateKernel, 16, Sizeof.cl_mem, Pointer.to(materialPropertiesBuffer));
        
        clEnqueueNDRangeKernel(commandQueue, updateKernel, 1, null,
                new long[]{workDimensions[0]}, null, 0, null, null);

        clEnqueueReadBuffer(commandQueue, persistentParticlesBuffer, CL_TRUE, 0,
                particles.length * Sizeof.cl_float, Pointer.to(particles),
                0, null, null);

        clEnqueueReadBuffer(commandQueue, temperaturesBuffer, CL_TRUE, 0,
            temperatures.length * Sizeof.cl_float, Pointer.to(temperatures), 0, null, null);

        return particles;
    }

    public void updateWorkSize(int size) {
        workDimensions[0] = size;
        if (persistentParticlesBuffer != null) {
            clReleaseMemObject(persistentParticlesBuffer);
            clReleaseMemObject(neighborIndicesBuffer);
            clReleaseMemObject(neighborCountsBuffer);
            clReleaseMemObject(temperaturesBuffer);
            clReleaseMemObject(materialIndicesBuffer);
            clReleaseMemObject(materialPropertiesBuffer);
        }
        persistentParticlesBuffer = null;
        neighborIndicesBuffer = null;
        neighborCountsBuffer = null;
        temperaturesBuffer = null;
        materialIndicesBuffer = null;
        materialPropertiesBuffer = null;
    }

    public void setDensity(double density) {
        float densityValue = (float) density;
        clSetKernelArg(updateKernel, 14, Sizeof.cl_float, Pointer.to(new float[]{densityValue}));
    }

    @Override
    public void close() {
        if (persistentParticlesBuffer != null) clReleaseMemObject(persistentParticlesBuffer);
        if (neighborIndicesBuffer != null) clReleaseMemObject(neighborIndicesBuffer);
        if (neighborCountsBuffer != null) clReleaseMemObject(neighborCountsBuffer);
        if (temperaturesBuffer != null) clReleaseMemObject(temperaturesBuffer);
        if (materialIndicesBuffer != null) clReleaseMemObject(materialIndicesBuffer);
        if (materialPropertiesBuffer != null) clReleaseMemObject(materialPropertiesBuffer);
        
        persistentParticlesBuffer = null;
        neighborIndicesBuffer = null;
        neighborCountsBuffer = null;
        temperaturesBuffer = null;
        materialIndicesBuffer = null;
        materialPropertiesBuffer = null;
    }
} 