#define MAX_NEIGHBORS 64  // Максимальное количество соседей для одной частицы
#define TEMPERATURE_DIFFUSION 0.1f  // Добавляем константу для скорости теплопередачи

// Объявляем функцию в начале файла
float random(int seed) {
    seed = (seed << 13) ^ seed;
    seed = (seed * (seed * seed * 15731 + 789221) + 1376312589) & 0x7fffffff;
    return (float)seed / 0x7fffffff;
}

__kernel void updateParticles(__global float* particles,
                            __global float* temperatures,  // Добавляем массив температур
                            __global int* neighborIndices,
                            __global int* neighborCounts,
                            const int width,
                            const int height,
                            const int2 mousePos,
                            const float mouseForce,
                            const float deltaTime,
                            const float viscosity,
                            const float repulsionStrength,
                            const float surfaceTension,
                            const float gravity,
                            const float baseMouseForce) {
    int gid = get_global_id(0);
    int pid = gid * 4;
    
    float2 pos = (float2)(particles[pid], particles[pid + 1]);
    float2 vel = (float2)(particles[pid + 2], particles[pid + 3]);
    
    // Взаимодействие только с соседями из хеш-таблицы
    float2 viscosityForce = (float2)(0, 0);
    float2 repulsionForce = (float2)(0, 0);
    float2 surfaceForce = (float2)(0, 0);
    float2 pressureForce = (float2)(0, 0);
    
    float interactionRadius = 30.0f;  // уменьшим радиус взаимодействия
    float interactionRadius2 = interactionRadius * interactionRadius;
    float minDistance = 10.0f;  // уменьшим минимальную дистанцию
    float restDensity = 15.0f;  // целевая плотность жидкости
    float pressureStrength = 200.0f;  // сила давления
    
    int startIdx = gid * MAX_NEIGHBORS;
    int count = neighborCounts[gid];
    
    // Сначала вычисляем локальную плотность
    float density = 0.0f;
    for(int n = 0; n < count; n++) {
        int i = neighborIndices[startIdx + n];
        if(i == gid) continue;
        
        int otherId = i * 4;
        float2 otherPos = (float2)(particles[otherId], particles[otherId + 1]);
        float2 diff = otherPos - pos;
        float dist2 = dot(diff, diff);
        
        if(dist2 < interactionRadius2 && dist2 > 0.0f) {
            float dist = sqrt(dist2);
            density += (1.0f - dist/interactionRadius);
        }
    }
    
    // Теплопередача между частицами
    float particleTemp = temperatures[gid];
    float tempDiff = 0.0f;
    
    for(int n = 0; n < count; n++) {
        int i = neighborIndices[startIdx + n];
        if(i == gid) continue;
        
        float otherTemp = temperatures[i];
        float2 diff = (float2)(
            particles[i*4] - pos.x,
            particles[i*4 + 1] - pos.y
        );
        float dist2 = dot(diff, diff);
        
        if(dist2 < interactionRadius2) {
            float influence = 1.0f - sqrt(dist2)/interactionRadius;
            tempDiff += (otherTemp - particleTemp) * influence * TEMPERATURE_DIFFUSION;
        }
    }
    
    // Обновляем температуру частицы
    temperatures[gid] = particleTemp + tempDiff * deltaTime;
    
    // Основной цикл взаимодействия
    for(int n = 0; n < count; n++) {
        int i = neighborIndices[startIdx + n];
        if(i == gid) continue;
        
        int otherId = i * 4;
        float2 otherPos = (float2)(particles[otherId], particles[otherId + 1]);
        float2 otherVel = (float2)(particles[otherId + 2], particles[otherId + 3]);
        float2 diff = otherPos - pos;
        float dist2 = dot(diff, diff);
        
        if(dist2 < interactionRadius2 && dist2 > 0.0f) {
            float dist = sqrt(dist2);
            float influence = 1.0f - dist/interactionRadius;
            
            // Вязкость
            float2 velDiff = otherVel - vel;
            viscosityForce += velDiff * influence * viscosity;
            
            // Отталкивание
            if(dist < minDistance) {
                float repulsionInfluence = 1.0f - dist/minDistance;
                repulsionForce -= normalize(diff) * repulsionStrength * repulsionInfluence;
            }
            
            // Давление для поддержания постоянной плотности
            float pressureForceStrength = pressureStrength * (density - restDensity) * influence;
            pressureForce -= normalize(diff) * pressureForceStrength;
            
            // Поверхностное натяжение (модифицированное)
            if(dist > interactionRadius * 0.7f) {  // только для частиц на поверхности
                surfaceForce += normalize(diff) * surfaceTension * influence;
            }
        }
    }
    
    // Влияние температуры на физику
    float tempFactor = (temperatures[gid] - 20.0f) / 80.0f;
    
    // Уменьшаем вязкость при высокой температуре
    float effectiveViscosity = viscosity * (1.0f - tempFactor * 0.8f);
    
    // Увеличиваем подвижность частиц при высокой температуре
    float2 thermalMotion = (float2)(
        (random(gid * 2) - 0.5f) * tempFactor * 100.0f,
        (random(gid * 2 + 1) - 0.5f) * tempFactor * 100.0f
    );
    
    // Применяем силы с учетом температуры
    vel += viscosityForce * effectiveViscosity * deltaTime;
    vel += repulsionForce * deltaTime;
    vel += surfaceForce * deltaTime;
    vel += pressureForce * deltaTime;
    vel += thermalMotion * deltaTime;
    
    // Применяем гравитацию
    vel.y += gravity * deltaTime;
    
    // Обработка взаимодействия с мышью
    if (mouseForce != 0) {
        float2 toMouse = (float2)(mousePos.x - pos.x, mousePos.y - pos.y);
        float dist = length(toMouse);
        float influence = 200.0f;
        
        if (dist < influence) {
            if (fabs(mouseForce) <= baseMouseForce) {  // Режим рисования
                float force = mouseForce * (1.0f - dist/influence);
                vel += normalize(toMouse) * force * deltaTime;
            } else if (fabs(mouseForce) > baseMouseForce * 2 && fabs(mouseForce) < baseMouseForce * 4) {  // Режим температуры
                // Ничего не делаем с движением частиц
            } else {  // Режим вихря
                float2 perpendicular = (float2)(-toMouse.y, toMouse.x);  // Перпендикулярный вектор
                float force = sign(mouseForce) * (1.0f - dist/influence) * 2000.0f;  // Увеличиваем силу вихря
                vel += normalize(perpendicular) * force * deltaTime;
            }
        }
    }
    
    // Более сильное затухание для холодной жидкости
    float dampingFactor = 0.98f + tempFactor * 0.01f;
    vel *= dampingFactor;
    
    // Обновление позиции
    pos += vel * deltaTime;
    
    // Коллизия со стенками
    float damping = 0.8f;
    float margin = 5.0f;
    
    if (pos.x < margin) {
        pos.x = margin;
        vel.x = -vel.x * damping;
    }
    if (pos.x > width - margin) {
        pos.x = width - margin;
        vel.x = -vel.x * damping;
    }
    if (pos.y < margin) {
        pos.y = margin;
        vel.y = -vel.y * damping;
    }
    if (pos.y > height - margin) {
        pos.y = height - margin;
        vel.y = -vel.y * damping;
    }
    
    // Сохранение результатов
    particles[pid] = pos.x;
    particles[pid + 1] = pos.y;
    particles[pid + 2] = vel.x;
    particles[pid + 3] = vel.y;
} 