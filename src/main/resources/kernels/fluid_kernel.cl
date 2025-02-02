#define MAX_NEIGHBORS 64  // Максимальное количество соседей для одной частицы
#define TEMPERATURE_DIFFUSION 0.1f  // Добавляем константу для скорости теплопередачи
#define MATERIAL_VISCOSITY_OFFSET 1  // Смещение для вязкости в массиве свойств
#define MATERIAL_SURFACE_TENSION_OFFSET 2  // Добавляем смещение для поверхностного натяжения
#define BUOYANCY_STRENGTH 9.81f  // Ускорение свободного падения

// Объявляем функцию в начале файла
float random(int seed) {
    seed = (seed << 13) ^ seed;
    seed = (seed * (seed * seed * 15731 + 789221) + 1376312589) & 0x7fffffff;
    return (float)seed / 0x7fffffff;
}

__kernel void updateParticles(
    __global float4* particles,
    __global float* temperatures,
    __global int* neighborIndices,
    __global int* neighborCounts,
    int width,
    int height,
    int2 mousePos,
    float mouseForce,
    float deltaTime,
    float viscosity,
    float repulsion,
    float surfaceTension,
    float gravity,
    float currentMouseForce,
    float density,  // Добавляем параметр плотности
    __global int* materialIndices,    // Индексы материалов для каждой частицы
    __global float* materialProperties // Свойства материалов (плотность, вязкость и т.д.)
) {
    int gid = get_global_id(0);
    int pid = gid * 4;
    
    float2 pos = (float2)(particles[gid].x, particles[gid].y);
    float2 vel = (float2)(particles[gid].z, particles[gid].w);
    
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
    
    int currentMaterial = materialIndices[gid];
    float currentDensity = materialProperties[currentMaterial * 4];  // Предполагаем 4 свойства на материал
    
    // Сначала вычисляем локальную плотность
    float localDensity = 0.0f;  // Переименовываем переменную
    for(int n = 0; n < count; n++) {
        int i = neighborIndices[startIdx + n];
        if(i == gid) continue;
        
        int otherId = i * 4;
        float2 otherPos = (float2)(particles[i].x, particles[i].y);
        float2 diff = otherPos - pos;
        float dist2 = dot(diff, diff);
        
        if(dist2 < interactionRadius2 && dist2 > 0.0f) {
            float dist = sqrt(dist2);
            localDensity += (1.0f - dist/interactionRadius);  // Используем новое имя
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
            particles[i].x - pos.x,
            particles[i].y - pos.y
        );
        float dist2 = dot(diff, diff);
        
        if(dist2 < interactionRadius2) {
            float influence = 1.0f - sqrt(dist2)/interactionRadius;
            tempDiff += (otherTemp - particleTemp) * influence * TEMPERATURE_DIFFUSION;
        }
    }
    
    // Обновляем температуру частицы
    temperatures[gid] = particleTemp + tempDiff * deltaTime;
    
    // Используем плотность материала для расчета массы частицы
    float mass = density;
    
    // Основной цикл взаимодействия
    for(int n = 0; n < count; n++) {
        int i = neighborIndices[startIdx + n];
        if(i == gid) continue;
        
        int otherId = i * 4;
        float2 otherPos = (float2)(particles[i].x, particles[i].y);
        float2 otherVel = (float2)(particles[i].z, particles[i].w);
        float2 diff = otherPos - pos;
        float dist2 = dot(diff, diff);
        
        if(dist2 < interactionRadius2 && dist2 > 0.0f) {
            float dist = sqrt(dist2);
            float influence = 1.0f - dist/interactionRadius;
            
            // Получаем свойства материалов
            int otherMaterial = materialIndices[i];
            float currentVisc = materialProperties[currentMaterial * 4 + MATERIAL_VISCOSITY_OFFSET];
            float otherVisc = materialProperties[otherMaterial * 4 + MATERIAL_VISCOSITY_OFFSET];
            float currentSurfaceTension = materialProperties[currentMaterial * 4 + MATERIAL_SURFACE_TENSION_OFFSET];
            float otherSurfaceTension = materialProperties[otherMaterial * 4 + MATERIAL_SURFACE_TENSION_OFFSET];
            
            // Вычисляем среднюю вязкость для взаимодействия
            float effectiveViscosity = (currentVisc + otherVisc) * 0.5f;
            
            // Применяем вязкость к разнице скоростей
            float2 velDiff = otherVel - vel;
            viscosityForce += velDiff * influence * effectiveViscosity * viscosity;
            
            // Отталкивание с учетом плотности
            if(dist < minDistance) {
                float repulsionInfluence = 1.0f - dist/minDistance;
                repulsionForce -= normalize(diff) * repulsion * repulsionInfluence * mass;
            }
            
            // Вычисляем среднюю плотность для взаимодействия
            float avgDensity = (currentDensity + materialProperties[otherMaterial * 4]) * 0.5f;
            
            // Модифицируем силы с учетом разницы плотностей
            float densityRatio = currentDensity / avgDensity;
            
            // Давление зависит от разницы плотностей
            float pressureForceStrength = pressureStrength * (localDensity - restDensity) * influence;
            pressureForce -= normalize(diff) * pressureForceStrength * densityRatio;
            
            // Вычисляем выталкивающую силу (закон Архимеда)
            float otherDensity = materialProperties[otherMaterial * 4];
            
            // Сила Архимеда = g * V * (ρ2 - ρ1)
            // где V - объём вытесненной жидкости (используем influence как приближение)
            // ρ2 - плотность окружающей жидкости
            // ρ1 - плотность текущей частицы
            float volumeDisplaced = influence * influence * influence;  // приближение объёма
            float densityDifference = otherDensity - currentDensity;
            
            // Применяем выталкивающую силу
            if(densityDifference != 0.0f) {
                float2 buoyancyForce = (float2)(0, BUOYANCY_STRENGTH * volumeDisplaced * densityDifference);
                
                // Добавляем боковую составляющую для лучшего разделения
                float2 horizontalForce = (float2)(
                    diff.x * 0.1f * fabs(densityDifference),
                    0
                );
                
                // Применяем силы с учетом расстояния
                float totalForce = length(buoyancyForce);
                float2 normalizedForce = normalize(buoyancyForce + horizontalForce);
                float2 finalForce = normalizedForce * totalForce * influence;
                
                // Добавляем к общим силам
                pressureForce += finalForce;
            }
            
            // Поверхностное натяжение зависит от обоих материалов
            if(dist > interactionRadius * 0.7f) {
                float effectiveSurfaceTension;
                if(currentMaterial == otherMaterial) {
                    // Для одинаковых материалов - сильное поверхностное натяжение
                    effectiveSurfaceTension = currentSurfaceTension;
                } else {
                    // Для разных материалов - среднее значение, умноженное на коэффициент смешивания
                    float mixFactor = 0.5f;  // Уменьшаем поверхностное натяжение между разными материалами
                    effectiveSurfaceTension = (currentSurfaceTension + otherSurfaceTension) * 0.5f * mixFactor;
                }
                surfaceForce += normalize(diff) * effectiveSurfaceTension * surfaceTension * influence;
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
    
    // Применяем силы с учетом температуры и массы
    // Делим все силы на массу при обновлении скорости (F = ma => a = F/m)
    vel += (viscosityForce * effectiveViscosity) * deltaTime / mass;
    vel += repulsionForce * deltaTime / mass;
    vel += surfaceForce * deltaTime / mass;
    vel += pressureForce * deltaTime / mass;
    vel += thermalMotion * deltaTime;  // thermal motion не зависит от массы
    
    // Гравитация не делится на массу (она уже дает правильное ускорение g)
    float2 gravityForce = (float2)(0, gravity);
    vel += gravityForce * deltaTime;
    
    // Обработка взаимодействия с мышью
    if (mouseForce != 0) {
        float2 toMouse = (float2)(mousePos.x - pos.x, mousePos.y - pos.y);
        float dist = length(toMouse);
        float influence = 200.0f;
        
        if (dist < influence) {
            if (fabs(mouseForce) <= currentMouseForce) {  // Режим рисования
                float force = mouseForce * (1.0f - dist/influence);
                vel += normalize(toMouse) * force * deltaTime;
            } else if (fabs(mouseForce) > currentMouseForce * 2 && fabs(mouseForce) < currentMouseForce * 4) {  // Режим температуры
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
    particles[gid] = (float4)(pos.x, pos.y, vel.x, vel.y);
} 