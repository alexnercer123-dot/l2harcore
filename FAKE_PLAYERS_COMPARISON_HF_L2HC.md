# Сравнительный анализ систем Fake Players в High Five и L2Hardcore

## Введение

В этом документе представлено сравнение систем Fake Players (фейковых игроков) в двух сборках сервера Lineage 2: стандартной High Five и модифицированной L2Hardcore. Fake Players - это NPC, имитирующие реальных игроков для улучшения атмосферы сервера и предоставления дополнительного контента.

## 1. Архитектура и реализация

### High Five

#### NPC Определения
- **Файлы**: `fpc_passive.xml` и `fpc_combat.xml` в директории `data/stats/npcs/custom/`
- **Типы NPC**: 
  - Пассивные (fpc_passive.xml) - не агрессивные NPC
  - Боевые (fpc_combat.xml) - агрессивные NPC с навыками

#### Структура NPC
```xml
<!-- Пассивный фейк-игрок в High Five -->
<npc id="80000" level="78" type="L2Npc" name="Evi">
    <status fakePlayer="true" talkable="false" undying="true" attackable="false" />
    <ai type="FIGHTER" clanHelpRange="1000" aggroRange="1000" isAggressive="true">
        <clanList>
            <clan>FAKE_PLAYER</clan>
        </clanList>
    </ai>
</npc>
```

#### Особенности:
- Используют атрибут `fakePlayer="true"` в элементе `<status>`
- Боевые NPC имеют элемент `<ai>` с параметрами агрессии
- Поддерживают навыки через `<parameters>` и `<skillList>`

### L2Hardcore

#### NPC Определения
- **Файлы**: `fpc_passive.xml` в директории `data/stats/npcs/custom/`
- **Типы NPC**: 
  - Все фейк-игроки определяются через специальный элемент `<fakePlayer>`

#### Структура NPC
```xml
<!-- Фейк-игрок в L2Hardcore -->
<npc id="80000" level="78" type="Folk" name="Evi">
    <status talkable="false" randomAnimation="false" randomWalk="false" undying="true" attackable="false" />
    <fakePlayer classId="107" hair="1" hairColor="0" face="0" equipRHand="5706" equipHead="2419" />
</npc>
```

#### Особенности:
- Используют специальный элемент `<fakePlayer>` с параметрами внешности
- Не используют атрибут `fakePlayer="true"` в `<status>`
- Все параметры внешности и экипировки задаются в элементе `<fakePlayer>`

## 2. Конфигурация

### High Five

#### Файл конфигурации: `FakePlayers.ini`
```ini
# Enable fake players.
EnableFakePlayers = False

# Aggressive AI fake players attack nearby monsters.
FakePlayerAggroMonsters = True

# Aggressive AI fake players attack nearby players.
FakePlayerAggroPlayers = False
```

#### Особенности:
- Базовые настройки включения/выключения системы
- Настройки агрессии для разных типов целей
- Использование шотов и дропа

### L2Hardcore

#### Файл конфигурации: `FakePlayers.ini`
```ini
# Our fake player system uses the existing NPC system, allowing fake players
# to function seamlessly like NPCs with minimal impact on server performance.

# Enable fake players.
EnableFakePlayers = True

# Fake players can be attacked without PvP flagging.
FakePlayerAutoAttackable = True
```

#### Особенности:
- Более подробное описание системы в комментариях
- Дополнительные настройки (например, [FakePlayerAutoAttackable](file://c:\project\l2hardcore\java\org\l2jmobius\gameserver\model\actor\Npc.java#L322-L325))
- Система оптимизирована для минимального влияния на производительность

## 3. Система спавна

### High Five

#### Файл спавна: `data/spawns/Others/FakePlayers.xml`
```xml
<spawn name="FakePlayers">
    <npc id="80000" x="83485" y="147998" z="-3407" heading="23509" respawnDelay="60" />
</spawn>
```

#### Особенности:
- Простая структура спавна
- Параметр `respawnDelay` для задания времени респауна

### L2Hardcore

#### Файл спавна: `data/spawns/Others/FakePlayers.xml`
```xml
<spawn name="FakePlayers">
    <group>
        <npc id="80000" x="83485" y="147998" z="-3407" heading="23509" respawnTime="60sec" />
        <npc id="80001" x="49544" y="40940" z="-3400" heading="0" respawnTime="30sec" />
    </group>
</spawn>
```

#### Особенности:
- Использование групп спавна
- Параметр `respawnTime` с указанием единиц измерения
- Более гибкая структура

## 4. Чат-система

### Общие особенности
Обе системы используют файл `FakePlayerChatData.xml` для определения ответов фейк-игроков на сообщения игроков.

#### Формат:
```xml
<fakePlayerChat fpcName="Evi" searchMethod="EQUALS" searchText="hello" answers="hello;hi;hi there" />
```

### Отличия

#### High Five
- Простая система с базовыми ответами
- Нет специфических ответов для разных фейк-игроков (кроме Evi)

#### L2Hardcore
- Расширенная система с индивидуальными ответами для каждого фейк-игрока
- Пример для нового Elf Warrior:
```xml
<fakePlayerChat fpcName="Aelien" searchMethod="EQUALS" searchText="hello" answers="Greetings, traveler!;Hello there!;Hi!" />
<fakePlayerChat fpcName="Aelien" searchMethod="CONTAINS" searchText="monster;around" answers="Yes, there are monsters nearby. I've been fighting them to improve my skills!;I'm keeping this area safe from monsters." />
```

## 5. Внешний вид и экипировка

### High Five
- Внешний вид определяется через экипировку (equipRHand, equipHead и т.д.)
- Для каждого предмета экипировки используется отдельный атрибут

### L2Hardcore
- Специальный элемент `<fakePlayer>` с параметрами внешности:
  - `classId` - ID класса персонажа
  - `hair`, `hairColor`, `face` - параметры прически и лица
  - `equipRHand`, `equipHead` и другие параметры экипировки

## 6. Боевая система

### High Five
- Поддержка двух типов фейк-игроков: пассивные и боевые
- Боевые NPC имеют навыки и параметры AI
- Используют элемент `<ai>` для настройки поведения

### L2Hardcore
- Все фейк-игроки могут быть агрессивными через настройки конфигурации
- Агрессия контролируется через `FakePlayerAggroMonsters = True`
- Более простая система без сложного AI

## 7. Преимущества и недостатки

### High Five

#### Преимущества:
- Поддержка разных типов фейк-игроков (пассивные/боевые)
- Система навыков для боевых NPC
- Гибкая настройка AI через элемент `<ai>`

#### Недостатки:
- Более сложная структура файлов
- Меньше параметров настройки внешности
- Нет индивидуальных ответов в чате для разных NPC

### L2Hardcore

#### Преимущества:
- Более простая и понятная структура
- Расширенные параметры настройки внешности
- Индивидуальные ответы в чате для каждого фейк-игрока
- Оптимизирована для минимального влияния на производительность

#### Недостатки:
- Нет поддержки разных типов фейк-игроков
- Нет системы навыков для фейк-игроков
- Менее гибкая настройка AI

## Заключение

Обе системы имеют свои особенности и подходят для разных целей:

- **High Five** предлагает более сложную и функциональную систему с поддержкой разных типов фейк-игроков и навыков
- **L2Hardcore** предоставляет более простую и оптимизированную систему с расширенными возможностями настройки внешности и индивидуальных ответов в чате

Выбор системы зависит от целей администратора сервера: для сложных PvE сценариев лучше подходит High Five, а для создания атмосферы сервера с минимальными затратами ресурсов - L2Hardcore.