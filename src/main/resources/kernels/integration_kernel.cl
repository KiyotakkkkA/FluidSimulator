__kernel void integrate(
    __global float4* particles,
    __global float2* forces,
    float deltaTime,
    float gravity,
    int2 mousePos,
    float mouseForce
) {
    int gid = get_global_id(0);
    float2 pos = (float2)(particles[gid].x, particles[gid].y);
    float2 vel = (float2)(particles[gid].z, particles[gid].w);
    
    vel += forces[gid] * deltaTime;
    vel += (float2)(0, gravity) * deltaTime;
    
    particles[gid] = (float4)(pos + vel * deltaTime, vel);
} 