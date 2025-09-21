# Система FakePlayers в L2J Mobius Hardcore

## Введение

Система FakePlayers (Фейковые Игроки) в данной сборке L2J Mobius Hardcore представляет собой механизм создания NPC, которые выглядят и ведут себя как настоящ Эти фейковые игроки могут взаимодействовать с настоящими игроками, атаковать, умирать, подбирать предметы и даже общаться в чате.

## Основные компоненты системы

### 1. Основные классы и файлы

#### `FakePlayerData.java`
- **Расположение**: `java/org/l2jmobius/gameserver/data/xml/FakePlayerData.java`
- **Функция**: Управляет данными фейковых игроков, хранит маппинги имен к NPC ID
- **Основные методы**:
  - `addFakePlayerId(String name, int npcId)` - добавляет связь имени с NPC ID
  - `getNpcIdByName(String name)` - получает NPC ID по имени
  - `getProperName(String name)` - получает правильное форматирование имени
  - `isTalkable(String name)` - проверяет, может ли фейковый игрок говорить

#### `FakePlayerHolder.java`
- **Расположение**: `java/org/l2jmobius/gameserver/model/actor/holders/npc/FakePlayerHolder.java`
- **Функция**: Хранит всю информацию о внешности и характеристиках фейкового игрока
- **Основные параметры**:
  - Класс персонажа (`PlayerClass`)
  - Внешность (прическа, цвет волос, лицо)
  - Экипировка (все слоты снаряжения)
  - Цвета имени и титула
  - Статус клана
  - Тип приватного магазина

#### `FakePlayerInfo.java`
- **Расположение**: `java/org/l2jmobius/gameserver/network/serverpackets/FakePlayerInfo.java`
- **Функция**: Сетевой пакет, отправляющий информацию о фейковом игроке клиенту
- **Особенности**: Использует тот же формат, что и пакет `CHAR_INFO` для настоящих игроков

#### `FakePlayerChatManager.java`
- **Расположение**: `java/org/l2jmobius/gameserver/managers/FakePlayerChatManager.java`
- **Функция**: Управляет системой чата фейковых игроков
- **Особенности**:
  - Поддерживает различные методы поиска сообщений (EQUALS, STARTS_WITH, CONTAINS)
  - Автоматические ответы с задержкой
  - Специальная логика для вопроса "can you see me"

### 2. Конфигурация

#### Файл конфигурации
- **Расположение**: `./config/Custom/FakePlayers.ini`
- **Основные настройки**:

```ini
# Включить/отключить систему фейковых игроков
EnableFakePlayers = false

# Включить чат фейковых игроков
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

### 3. Создание фейковых игроков

#### XML конфигурация в npc.xml
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

### 4. Система чата

#### Конфигурация чата (FakePlayerChatData.xml)
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

#### Методы поиска сообщений:
- `EQUALS` - точное совпадение
- `STARTS_WITH` - сообщение начинается с текста
- `CONTAINS` - сообщение содержит все указанные слова

## Поведение и AI

### 1. Боевая система

#### Агрессия и PvP
- Фейковые игроки могут атаковать настоящих игроков и монстров
- Поддерживается система флагинга (PvP флаг и карма)
- Убийство фейкового игрока может давать PvP очки или карму

#### Особенности боя
- Фейковые игроки могут использовать soulshots/spiritshots
- Поддерживается система критических ударов
- Агро система работает аналогично монстрам

### 2. Система дропа и подбора предметов

```java
// При смерти монстра
if (Config.FAKE_PLAYER_CAN_PICKUP) {
    mainDamageDealer.getFakePlayerDrops().add(droppedItem);
}

// В AI фейкового игрока
if (!npc.isInCombat()) {
    final int itemIndex = npc.getFakePlayerDrops().size() - 1;
    final Item droppedItem = npc.getFakePlayerDrops().get(itemIndex);
    if ((droppedItem != null) && droppedItem.isSpawned()) {
        if (npc.calculateDistance2D(droppedItem) > 50) {
            moveTo(droppedItem);
        } else {
            npc.getFakePlayerDrops().remove(itemIndex);
            droppedItem.pickupMe(npc);
        }
    }
}
```

### 3. Взаимодействие с игроками

#### Ответы на сообщения
- Поддержка приватных сообщений
- Автоматические ответы с рандомной задержкой (5-15 секунд)
- Специальная логика видимости ("can you see me")

#### Социальные функции
- Приглашения в группу
- Приглашения в клан
- Блокировка игроков
- Приватные магазины

## Технические особенности

### 1. Сетевые пакеты

Фейковые игроки используют специальный пакет `FakePlayerInfo` вместо стандартного `NpcInfo`:

```java
@Override
public void sendInfo(Player player) {
    if (isVisibleFor(player)) {
        if (_isFakePlayer) {
            player.sendPacket(new FakePlayerInfo(this));
            // Поддержка сообщений приватного магазина
        } else {
            // Обычная логика NPC
        }
    }
}
```

### 2. Обновления состояния

При изменении состояния фейкового игрока отправляется соответствующий пакет:

```java
// При изменении скорости движения
if (isFakePlayer()) {
    player.sendPacket(new FakePlayerInfo(asNpc()));
}

// При изменении аномальных эффектов
if (_isFakePlayer) {
    player.sendPacket(new FakePlayerInfo(this));
}
```

### 3. Совместимость с системами

#### Зоны
- Корректная работа в водных зонах
- Поддержка PvP зон
- Респаун система

#### События
- Интеграция с системой событий
- Поддержка скриптов
- Система квестов

## Примеры использования

### 1. Создание городского NPC-игрока

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

### 2. Боевой фейковый игрок

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

### 3. Настройка чата

```xml
<fakePlayerChat fpcName="Trader_John" 
                searchMethod="CONTAINS" 
                searchText="weapon;sword;armor" 
                answers="Check my store for the best equipment!;I have what you need!" />
```

## Отладка и мониторинг

### Логирование
```
FakePlayerData: Loaded X templates.
FakePlayerChatManager: Loaded X chat templates.
```

### Полезные команды
- Проверка активных фейковых игроков
- Мониторинг чата
- Анализ взаимодействий

## Заключение

Система FakePlayers в L2J Mobius Hardcore предоставляет мощный инструмент для создания живого игрового мира. Фейковые игроки могут служить различными целями:

- **Торговцы** - создание атмосферы оживленного рынка
- **Охранники** - защита городов и важных локаций  
- **Компаньоны** - помощь новым игрокам
- **Противники** - создание PvP контента

Система полностью интегрирована с основными механиками сервера и может быть легко настроена под нужды конкретного сервера.