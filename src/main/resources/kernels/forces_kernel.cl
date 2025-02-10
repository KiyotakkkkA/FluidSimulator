__kernel void computeForces(
    __global float4* particles,
    __global float* localDensities,
    __global int* neighborIndices,
    __global int* neighborCounts,
    __global int* materialIndices,
    __global float* materialProperties,
    __global float2* forces,
    float viscosity,
    float repulsion,
    float surfaceTension,
    float deltaTime,
    __local float4* localParticles,
    __local float* localDensities
) {
    int gid = get_global_id(0);
    int lid = get_local_id(0);
    
    localParticles[lid] = particles[gid];
    localDensities[lid] = localDensities[gid];
    barrier(CLK_LOCAL_MEM_FENCE);
    
    float2 pos = (float2)(particles[gid].x, particles[gid].y);
    float2 vel = (float2)(particles[gid].z, particles[gid].w);
    
    float2 viscosityForce = 0.0f;
    float2 pressureForce = 0.0f;
    float2 surfaceForce = 0.0f;
    
    int currentMaterial = materialIndices[gid];
    float currentDensity = materialProperties[currentMaterial * 4];
    float currentVisc = materialProperties[currentMaterial * 4 + MATERIAL_VISCOSITY_OFFSET];
    
    int startIdx = gid * MAX_NEIGHBORS;
    int count = neighborCounts[gid];
    
    forces[gid] = viscosityForce + pressureForce + surfaceForce;
} 