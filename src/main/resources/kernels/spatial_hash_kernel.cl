#define CELL_SIZE 30.0f
#define MAX_NEIGHBORS 64

__kernel void buildSpatialHash(__global float* particles,
                             __global int* neighborIndices,
                             __global int* neighborCounts,
                             const int numParticles,
                             const float searchRadius) {
    int gid = get_global_id(0);
    if (gid >= numParticles) return;
    
    float2 pos = (float2)(particles[gid * 4], particles[gid * 4 + 1]);
    
    float searchDist = searchRadius + CELL_SIZE;
    int2 minCell = convert_int2((pos - searchDist) / CELL_SIZE);
    int2 maxCell = convert_int2((pos + searchDist) / CELL_SIZE);
    
    int neighborCount = 0;
    int startIdx = gid * MAX_NEIGHBORS;
    
    for (int i = 0; i < numParticles && neighborCount < MAX_NEIGHBORS; i++) {
        if (i == gid) continue;
        
        float2 otherPos = (float2)(particles[i * 4], particles[i * 4 + 1]);
        float2 diff = otherPos - pos;
        float dist2 = dot(diff, diff);
        
        if (dist2 < searchRadius * searchRadius) {
            neighborIndices[startIdx + neighborCount] = i;
            neighborCount++;
        }
    }
    
    neighborCounts[gid] = neighborCount;
} 