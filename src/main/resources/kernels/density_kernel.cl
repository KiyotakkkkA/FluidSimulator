__kernel void computeDensity(
    __global float4* particles,
    __global float* localDensities,
    __global int* neighborIndices,
    __global int* neighborCounts,
    float interactionRadius,
    __local float4* localParticles
) {
    int gid = get_global_id(0);
    int lid = get_local_id(0);
    int groupSize = get_local_size(0);
    
    localParticles[lid] = particles[gid];
    barrier(CLK_LOCAL_MEM_FENCE);
    
    float2 pos = (float2)(particles[gid].x, particles[gid].y);
    float localDensity = 0.0f;
    float interactionRadius2 = interactionRadius * interactionRadius;
    
    int startIdx = gid * MAX_NEIGHBORS;
    int count = neighborCounts[gid];
    
    for(int n = 0; n < count; n += 4) {
        int4 indices = vload4(0, neighborIndices + startIdx + n);
        float4 densitySum = 0.0f;
        
        #pragma unroll
        for(int i = 0; i < 4; i++) {
            if(n + i >= count) break;
            int idx = indices[i];
            float2 otherPos = (float2)(particles[idx].x, particles[idx].y);
            float2 diff = otherPos - pos;
            float dist2 = dot(diff, diff);
            
            if(dist2 < interactionRadius2) {
                densitySum[i] = 1.0f - sqrt(dist2)/interactionRadius;
            }
        }
        localDensity += densitySum.x + densitySum.y + densitySum.z + densitySum.w;
    }
    
    localDensities[gid] = localDensity;
} 