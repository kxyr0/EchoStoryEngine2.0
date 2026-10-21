'use strict';

const QUEST_STATUS_DETAILS = {
  active: {
    label: 'ACTIVE',
    title: 'Active quest',
    color: 'rgba(86, 156, 214, 0.95)',
    backgroundColor: 'rgba(86, 156, 214, 0.18)',
    borderColor: 'rgba(86, 156, 214, 0.75)'
  },
  completed: {
    label: 'COMPLETED',
    title: 'Completed quest',
    color: 'rgba(78, 201, 176, 0.95)',
    backgroundColor: 'rgba(78, 201, 176, 0.18)',
    borderColor: 'rgba(78, 201, 176, 0.75)'
  },
  incomplete: {
    label: 'INCOMPLETE',
    title: 'Incomplete quest',
    color: 'rgba(220, 220, 170, 0.95)',
    backgroundColor: 'rgba(220, 220, 170, 0.16)',
    borderColor: 'rgba(220, 220, 170, 0.65)'
  },
  failed: {
    label: 'FAILED',
    title: 'Failed quest',
    color: 'rgba(244, 71, 71, 0.95)',
    backgroundColor: 'rgba(244, 71, 71, 0.18)',
    borderColor: 'rgba(244, 71, 71, 0.75)'
  },
  locked: {
    label: 'LOCKED',
    title: 'Locked quest',
    color: 'rgba(197, 134, 192, 0.95)',
    backgroundColor: 'rgba(197, 134, 192, 0.16)',
    borderColor: 'rgba(197, 134, 192, 0.65)'
  }
};

function questVisualsInText(text) {
  const source = String(text || '');
  const quests = [];
  const pattern = /\btype\s*:\s*(['"])quest_update\1/gi;
  for (const match of source.matchAll(pattern)) {
    const objectStart = source.lastIndexOf('{', match.index);
    const objectEnd = objectStart >= 0 ? matchingBracket(source, objectStart) : -1;
    if (objectStart < 0 || objectEnd < match.index) {
      continue;
    }

    const fields = stringFieldsInRange(source, objectStart, objectEnd);
    const statusField = fields.find(field => field.name === 'status');
    const status = normalizeQuestStatus(statusField && statusField.value);
    const questId = valueForField(fields, 'questId', 'missionId', 'id');
    const stageId = valueForField(fields, 'stageId', 'stage');
    const title = valueForField(fields, 'title', 'titleKey');
    const stageTitle = valueForField(fields, 'stageTitle', 'stageTitleKey');
    const typeStart = match.index + match[0].lastIndexOf('quest_update');

    quests.push({
      status,
      statusLabel: QUEST_STATUS_DETAILS[status].label,
      questId,
      stageId,
      title,
      stageTitle,
      objectRange: { start: objectStart, end: objectEnd + 1 },
      typeRange: { start: typeStart, end: typeStart + 'quest_update'.length },
      statusRange: statusField ? statusField.valueRange : undefined
    });
  }
  return quests.sort((left, right) => left.objectRange.start - right.objectRange.start);
}

function normalizeQuestStatus(value) {
  const normalized = String(value || '').trim().toLowerCase();
  if (['active', 'started', 'start', 'in_progress', 'in-progress', 'progress'].includes(normalized)) {
    return 'active';
  }
  if (['completed', 'complete', 'done', 'finished', 'finish'].includes(normalized)) {
    return 'completed';
  }
  if (['failed', 'fail', 'failure'].includes(normalized)) {
    return 'failed';
  }
  if (['locked', 'lock', 'blocked'].includes(normalized)) {
    return 'locked';
  }
  return 'incomplete';
}

function questCodeLensTitle(quest) {
  const parts = [
    `Quest ${quest.statusLabel}`,
    quest.questId || 'unknown quest'
  ];
  if (quest.stageId) {
    parts.push(`stage ${quest.stageId}`);
  }
  return parts.join(' - ');
}

function questHoverLines(quest) {
  const detail = QUEST_STATUS_DETAILS[quest.status];
  return [
    `**${detail.title}: \`${quest.questId || 'unknown'}\`**`,
    quest.status === 'incomplete' ? 'No explicit `status` field was found.' : `Status: \`${quest.statusLabel}\`.`,
    quest.stageId ? `Stage: \`${quest.stageId}\`.` : '',
    quest.title ? `Title: \`${quest.title}\`.` : '',
    quest.stageTitle ? `Stage title: \`${quest.stageTitle}\`.` : ''
  ].filter(Boolean);
}

function valueForField(fields, ...names) {
  const field = fields.find(entry => names.includes(entry.name));
  return field ? field.value : '';
}

function stringFieldsInRange(text, start, end) {
  const result = [];
  const pattern = /\b([A-Za-z_$][\w$-]*)\s*:\s*(['"])(.*?)\2/g;
  const slice = text.slice(start, end + 1);
  for (const match of slice.matchAll(pattern)) {
    const valueStart = start + match.index + match[0].indexOf(match[3]);
    result.push({
      name: match[1],
      value: match[3],
      nameRange: {
        start: start + match.index + match[0].indexOf(match[1]),
        end: start + match.index + match[0].indexOf(match[1]) + match[1].length
      },
      valueRange: {
        start: valueStart,
        end: valueStart + match[3].length
      }
    });
  }
  return result;
}

function matchingBracket(text, start) {
  const open = text[start];
  const close = open === '{' ? '}' : open === '[' ? ']' : '';
  if (!close) {
    return -1;
  }

  let depth = 0;
  let index = start;
  while (index < text.length) {
    const char = text[index];
    if (char === '"' || char === "'") {
      index = skipString(text, index);
      continue;
    }
    if (char === '/' && text[index + 1] === '/') {
      index = skipLineComment(text, index);
      continue;
    }
    if (char === '/' && text[index + 1] === '*') {
      index = skipBlockComment(text, index);
      continue;
    }
    if (char === open) {
      depth += 1;
    } else if (char === close) {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
    }
    index += 1;
  }
  return -1;
}

function skipString(text, start) {
  const quote = text[start];
  let index = start + 1;
  while (index < text.length) {
    if (text[index] === '\\') {
      index += 2;
      continue;
    }
    if (text[index] === quote) {
      return index + 1;
    }
    index += 1;
  }
  return index;
}

function skipLineComment(text, start) {
  let index = start + 2;
  while (index < text.length && text[index] !== '\n') {
    index += 1;
  }
  return index;
}

function skipBlockComment(text, start) {
  let index = start + 2;
  while (index + 1 < text.length && !(text[index] === '*' && text[index + 1] === '/')) {
    index += 1;
  }
  return Math.min(text.length, index + 2);
}

module.exports = {
  QUEST_STATUS_DETAILS,
  questVisualsInText,
  normalizeQuestStatus,
  questCodeLensTitle,
  questHoverLines
};
