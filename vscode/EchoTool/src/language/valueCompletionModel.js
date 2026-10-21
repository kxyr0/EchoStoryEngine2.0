'use strict';

const BOOLEAN_FIELDS = new Set([
  'allowCamera',
  'allowInteraction',
  'allowMovement',
  'attachToHand',
  'autoEquip',
  'canFloat',
  'debug',
  'drop',
  'dropPrevious',
  'drop_previous',
  'enabled',
  'enterBuilding',
  'exit',
  'exitOnArrival',
  'external',
  'face',
  'facePlayer',
  'fireOnce',
  'holdCamera',
  'keepCamera',
  'keepCameraAfterTrip',
  'keepItem',
  'keepPreviousItem',
  'keepReplacedItem',
  'leaveVehicle',
  'lockControls',
  'lockMouse',
  'lockPlayer',
  'look',
  'lookAtPlayer',
  'loop',
  'openDoors',
  'openExternal',
  'replacePreferredSlot',
  'replayable',
  'resetAttempts',
  'reset_attempts',
  'respectCooldown',
  'showNotification',
  'skippable',
  'solved',
  'teleportToTargetIfStuck',
  'thought',
  'usePathfinding',
  'visible',
  'walk',
  'walkToBuilding',
  'warpWhenUnseen'
]);

const LOCALIZATION_KEY_FIELDS = new Set([
  'bodyKey',
  'buttonKey',
  'completedLabelKey',
  'displayNameKey',
  'labelKey',
  'markerLabelKey',
  'nameKey',
  'stageTitleKey',
  'successLabelKey',
  'textKey',
  'titleKey',
  'unavailableLabelKey'
]);

const ENUM_FIELD_MAP = new Map([
  ['as', 'vehicleRole'],
  ['backend', 'vehicleBackend'],
  ['cameraCollision', 'cameraCollisionPolicy'],
  ['cameraCollisionPolicy', 'cameraCollisionPolicy'],
  ['category', 'vehicleCategory'],
  ['cinematicCompletionPolicy', 'cinematicCompletionPolicy'],
  ['collisionPolicy', 'cameraCollisionPolicy'],
  ['completionPolicy', 'cinematicCompletionPolicy'],
  ['control', 'vehicleBackend'],
  ['driveBackend', 'vehicleBackend'],
  ['endPolicy', 'cinematicCompletionPolicy'],
  ['ghostCamera', 'cameraCollisionPolicy'],
  ['motionBackend', 'vehicleBackend'],
  ['role', 'vehicleRole'],
  ['seatRole', 'vehicleRole'],
  ['seatType', 'vehicleRole'],
  ['vehicleCategory', 'vehicleCategory'],
  ['vehicleRole', 'vehicleRole']
]);

function scalarValuesForField(catalog, fieldName) {
  if (isBooleanField(fieldName)) {
    return booleanValues(catalog);
  }
  const enumName = ENUM_FIELD_MAP.get(String(fieldName || ''));
  if (enumName) {
    return enumValues(catalog, enumName);
  }
  return [];
}

function booleanValues(catalog) {
  return enumValues(catalog, 'boolean', ['true', 'false']);
}

function enumValues(catalog, enumName, fallback = []) {
  const values = catalog && catalog.enums && catalog.enums.boolean;
  if (enumName === 'boolean') {
    return Array.isArray(values) && values.length > 0 ? values : fallback;
  }
  const valuesForEnum = catalog && catalog.enums && catalog.enums[enumName];
  return Array.isArray(valuesForEnum) && valuesForEnum.length > 0 ? valuesForEnum : fallback;
}

function isBooleanField(fieldName) {
  return BOOLEAN_FIELDS.has(String(fieldName || ''));
}

function isLocalizationKeyField(fieldName) {
  return LOCALIZATION_KEY_FIELDS.has(String(fieldName || ''));
}

module.exports = {
  BOOLEAN_FIELDS,
  LOCALIZATION_KEY_FIELDS,
  scalarValuesForField,
  isBooleanField,
  isLocalizationKeyField
};
