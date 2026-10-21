'use strict';

const STATIC_NAMESPACE_MEMBERS = {
  FaceLayer: [
    ['Base', 'base'],
    ['Overlay', 'overlay'],
    ['Detail', 'detail']
  ],
  Hand: [
    ['Main', 'mainhand']
  ],
  Items: [
    ['SparkGrass', 'echostoryengine:spark_grass']
  ],
  Layer: [
    ['Base', 'base'],
    ['Overlay', 'overlay']
  ],
  EquipmentSlot: [
    ['Head', 'HEAD'],
    ['Chest', 'CHEST'],
    ['Legs', 'LEGS'],
    ['Feet', 'FEET']
  ],
  NameMode: [
    ['Account', 'ACCOUNT'],
    ['Custom', 'CUSTOM']
  ],
  Rarity: [
    ['Common', 'COMMON'],
    ['Uncommon', 'UNCOMMON'],
    ['Rare', 'RARE'],
    ['Epic', 'EPIC']
  ],
  SkinMode: [
    ['Account', 'ACCOUNT'],
    ['Resource', 'RESOURCE']
  ],
  SkinModel: [
    ['Classic', 'CLASSIC'],
    ['Slim', 'SLIM']
  ],
  OldFriend: [
    ['Started', 'chapter_1.j_old_friend_started'],
    ['MoneyGiven', 'chapter_1.j_old_friend_money_given'],
    ['ItemReady', 'chapter_1.j_old_friend_item_ready'],
    ['Paid', 'chapter_1.j_old_friend_paid'],
    ['Quest', 'j_old_friend'],
    ['GoToFriendStage', 'j_old_friend_point']
  ],
  Portraits: [
    ['Ben', 'ben'],
    ['Buyer', 'buyer'],
    ['Dzenna', 'dzenna'],
    ['JohnFather', 'john_father'],
    ['Lena', 'lena'],
    ['Lololoshka', 'lololoshka'],
    ['Marlo', 'marlo'],
    ['Molli', 'molli'],
    ['Rozental', 'rozental'],
    ['Saimonsolus', 'saimonsolus'],
    ['Sasha', 'sasha'],
    ['Smotriashchii', 'smotriashchii']
  ],
  QuestStatus: [
    ['Active', 'ACTIVE'],
    ['Completed', 'COMPLETED']
  ],
  States: [
    ['Idle', 'idle'],
    ['Patrol', 'patrol']
  ],
  TriggerMode: [
    ['Once', 'ONCE'],
    ['Repeat', 'REPEAT']
  ],
  TriggerScope: [
    ['Global', 'GLOBAL'],
    ['Server', 'SERVER'],
    ['Player', 'PLAYER'],
    ['DialogueSession', 'DIALOGUE_SESSION']
  ],
  TriggerStayPolicy: [
    ['SkipWhileRunning', 'SKIP_WHILE_RUNNING'],
    ['QueueOne', 'QUEUE_ONE']
  ]
};

const NAMED_STATIC_NAMESPACES = {
  'Npc.ClearHeldItem': {
    hand: ['Hand']
  },
  'Npc.HoldItem': {
    hand: ['Hand']
  },
  'Npc.PlayAnimation': {
    layer: ['Layer']
  },
  'Npc.PlayFaceAnimation': {
    layer: ['FaceLayer']
  },
  'Npc.StopAnimation': {
    layer: ['Layer']
  },
  'Npc.StopFaceAnimation': {
    layer: ['FaceLayer']
  },
  PlayerProfile: {
    nameMode: ['NameMode'],
    skinMode: ['SkinMode'],
    skinModel: ['SkinModel']
  },
  'Quest.Update': {
    status: ['QuestStatus']
  },
  Scope: {
    scope: ['TriggerScope']
  },
  Mode: {
    mode: ['TriggerMode']
  },
  StayPolicy: {
    policy: ['TriggerStayPolicy']
  },
  'This.ClearHeldItem': {
    hand: ['Hand']
  },
  'This.HoldItem': {
    hand: ['Hand']
  },
  'This.PlayAnimation': {
    layer: ['Layer']
  },
  'This.StopAnimation': {
    layer: ['Layer']
  },
  Weapon: {
    rarity: ['Rarity'],
    equipmentSlot: ['EquipmentSlot']
  }
};

module.exports = {
  NAMED_STATIC_NAMESPACES,
  STATIC_NAMESPACE_MEMBERS
};
