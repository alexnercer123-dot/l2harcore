# Возможности Фейковых Игроков в сборке L2J Mobius Hardcore

## Введение

Система фейковых игроков в сборке L2J Mobius Hardcore предоставляет мощный инструмент для создания NPC, которые выглядят и ведут себя как настоящие игроки. Эти фейковые игроки могут взаимодействовать с реальными игроками, участвовать в бою, подбирать предметы и даже общаться в чате.

## Основные Возможности

### 1. Визуальное Представление
- Фейковые игроки отображаются как полноценные персонажи с настраиваемой внешностью
- Поддержка всех слотов экипировки (оружие, доспехи, украшения)
- Настраиваемые параметры внешности: причёска, цвет волос, лицо
- Возможность установки цветов имени и титула
- Поддержка эффектов рыбалки и надевания агатионов

### 2. Боевые Системы
- Фейковые игроки могут атаковать монстров и реальных игроков
- Поддержка использования зарядов (soulshots/spiritshots)
- Система агрессии к монстрам, игрокам и другим фейковым игрокам
- Возможность получения PvP флага и кармы при убийствах
- Поддержка критических ударов и других боевых механик

### 3. Система Чата
- Автоматические ответы на сообщения игроков
- Настраиваемые шаблоны ответов с поддержкой нескольких вариантов
- Три метода поиска сообщений:
  - `EQUALS` - точное совпадение
  - `STARTS_WITH` - сообщение начинается с текста
  - `CONTAINS` - сообщение содержит указанные слова
- Специальная логика для вопроса "can you see me"
- Задержка ответов (5-15 секунд) для более реалистичного поведения

### 4. Экономические Функции
- Поддержка приватных магазинов (продажа/покупка)
- Возможность подбирать предметы с земли
- Возможность дропать предметы при смерти
- Настройка сообщений приватных магазинов

### 5. Социальные Функции
- Отображение в списке игроков как обычных персонажей
- Поддержка клановой системы (ID клана, статус в клане)
- Возможность получения рекомендаций
- Поддержка статуса героя и уровней благородства
- Возможность сидеть/стоять

## Конфигурация

### Основные Настройки (FakePlayers.ini)
```
# Включение/выключение системы фейковых игроков
EnableFakePlayers = false

# Включение чата фейковых игроков
FakePlayerChat = false

# Использование зарядов (soulshots/spiritshots)
FakePlayerUseShots = false

# Награды PvP за убийство фейковых игроков
FakePlayerKillsRewardPvP = false

# Карма за убийство незафлаженных фейковых игроков
FakePlayerUnflaggedKillsKarma = false

# Автоматическая атакуемость фейковых игроков
FakePlayerAutoAttackable = false

# Агрессия фейковых игроков к монстрам
FakePlayerAggroMonsters = false

# Агрессия фейковых игроков к игрокам
FakePlayerAggroPlayers = false

# Агрессия фейковых игроков друг к другу
FakePlayerAggroFPC = false

# Возможность дропать предметы
FakePlayerCanDropItems = false

# Возможность подбирать предметы
FakePlayerCanPickup = false
```

## Создание Фейковых Игроков

Фейковые игроки создаются через XML конфигурацию в npc.xml:

```xml
<npc id="YOUR_NPC_ID" name="PlayerName" title="" type="FakePlayer">
    <set name="level" value="80"/>
    <set name="hp" value="3000"/>
    <set name="mp" value="1500"/>
    <!-- другие базовые характеристики -->
    
    <fakeplayer classId="88" 
               hair="1" 
               hairColor="8" 
               face="0" 
               nameColor="0xFFFFFF" 
               titleColor="0xECF9A2"
               equipHead="0" 
               equipRHand="7883" 
               equipLHand="0" 
               equipGloves="0" 
               equipChest="2384" 
               equipLegs="2388" 
               equipFeet="2403" 
               equipCloak="0"
               equipHair="0" 
               equipHair2="0" 
               agathionId="0"
               weaponEnchantLevel="0" 
               armorEnchantLevel="0"
               fishing="false" 
               baitLocationX="0" 
               baitLocationY="0" 
               baitLocationZ="0"
               recommends="0" 
               nobleLevel="0" 
               hero="false" 
               clanId="0" 
               pledgeStatus="0"
               sitting="false" 
               privateStoreType="0" 
               privateStoreMessage=""
               fakePlayerTalkable="true" />
</npc>
```

## Настройка Чата

Конфигурация чата осуществляется через XML файл:

```xml
<list>
    <fakePlayerChat fpcName="PlayerName" 
                    searchMethod="CONTAINS" 
                    searchText="hello;hi" 
                    answers="Hello there!;Hi!" />
    
    <fakePlayerChat fpcName="ALL" 
                    searchMethod="EQUALS" 
                    searchText="how are you" 
                    answers="I'm fine, thanks!" />
</list>
```

## Технические Особенности

### Сетевые Пакеты
- Использование специального пакета `FakePlayerInfo` вместо стандартного `NpcInfo`
- Поддержка всех визуальных эффектов и анимаций настоящих игроков
- Корректная синхронизация состояния между клиентом и сервером

### Совместимость
- Полная интеграция с системой зон (PvP, водные зоны)
- Поддержка событий и квестов
- Совместимость с системой респауна

## Как Включить и Добавить Фейковых Игроков

### 1. Включение Системы

1. Откройте файл конфигурации:
   `dist/game/config/Custom/FakePlayers.ini`

2. Установите следующие параметры:
   ```
   EnableFakePlayers = True
   FakePlayerChat = True
   ```

3. При необходимости настройте другие параметры системы фейковых игроков.

### 2. Добавление Фейковых Игроков на Локации

Фейковые игроки добавляются через систему спавна, аналогично обычным NPC:

1. Определите фейкового игрока в файле статистики NPC:
   `dist/game/data/stats/npcs/custom/fpc_passive.xml`
   
   Пример определения:
   ```xml
   <npc id="80000" level="78" type="Folk" name="Evi">
       <race>DARK_ELF</race>
       <sex>FEMALE</sex>
       <stats str="103" int="40" dex="53" wit="37" con="84" men="39">
           <vitals hp="18857" hpRegen="10.8" mp="3863" mpRegen="3.0" />
           <attack physical="10198" magical="1305" random="30" critical="132" accuracy="177" attackSpeed="588" type="FIST" range="40" distance="80" width="120" />
           <defence physical="3388" magical="2450" />
           <attribute>
               <defence fire="200" water="250" wind="250" earth="250" holy="250" dark="250" />
               <attack type="WATER" value="330" />
           </attribute>
           <speed>
               <walk ground="85" />
               <run ground="139" />
           </speed>
       </stats>
       <status talkable="false" randomAnimation="false" randomWalk="false" undying="true" attackable="false" />
       <fakePlayer classId="107" hair="1" hairColor="0" face="0" equipRHand="5706" equipHead="2419" equipGloves="5774" equipChest="2383" equipFeet="5786" />
       <collision>
           <radius normal="7.5" />
           <height normal="24" />
       </collision>
   </npc>
   ```

2. Добавьте спавн фейкового игрока в соответствующий файл локации:
   `dist/game/data/spawns/Others/FakePlayers.xml`
   
   Пример спавна:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <list xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="../../xsd/spawns.xsd">
       <spawn name="FakePlayers">
           <group>
               <npc id="80000" x="83485" y="147998" z="-3407" heading="23509" respawnTime="60sec" /> <!-- Evi -->
           </group>
       </spawn>
   </list>
   ```

3. При необходимости настройте чатовые ответы в файле:
   `dist/game/data/FakePlayerChatData.xml`

### 3. Перезапуск Сервера

После внесения изменений перезапустите сервер для применения настроек.

## Примеры Использования

### 1. Городской Торговец
```xml
<npc id="50001" name="Trader_John" title="Weapon Seller" type="FakePlayer">
    <fakeplayer classId="88" 
               equipRHand="7883" 
               equipChest="2384" 
               privateStoreType="1" 
               privateStoreMessage="Best weapons in town!"
               fakePlayerTalkable="true" />
</npc>
```

### 2. Боевой Страж
```xml
<npc id="50002" name="Guardian_Alex" title="City Guard" type="FakePlayer">
    <fakeplayer classId="2" 
               equipRHand="6" 
               equipLHand="10" 
               equipChest="354"
               weaponEnchantLevel="5" 
               armorEnchantLevel="3" />
</npc>
```

## Заключение

Система фейковых игроков в L2J Mobius Hardcore предоставляет гибкий и мощный инструмент для оживления игрового мира. Фейковые игроки могут выполнять различные функции:

- **Торговцы** - создание атмосферы оживленного рынка
- **Охранники** - защита городов и важных локаций  
- **Компаньоны** - помощь новым игрокам
- **Противники** - создание PvP контента

Система полностью интегрирована с основными механиками сервера и может быть легко настроена под нужды конкретного сервера.