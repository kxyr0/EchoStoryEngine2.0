'use strict';

// Generated from EventCatalogDescriptor by echo-events EventCatalogExport.
const catalog = require('../catalog/echoEvents.json');

function eventCompletions(before, index) {
  if (!/\bWhen\s*\([^;{}]*$/s.test(before)) return null;
  const member = /\b(Dialogue|Scene|Cutscene|Observation|Npc|Animation)\.\s*$/.exec(before);
  if (member) return catalog.filter(value => value.predicate.startsWith(`${member[1]}.`)).map(value => {
    const method = value.predicate.split('.')[1];
    return { label: method, insertText: `${method}(`, kind: 'method',
      detail: `${value.predicate}: ${value.filters.join(', ')}; ${value.scope}`, sortText: `000_event_${method}` };
  });
  const call = /\b([A-Za-z]+\.[A-Za-z]+)\(\s*([^()]*)$/.exec(before);
  const descriptor = call && catalog.find(value => value.predicate === call[1]);
  if (!descriptor || !index || typeof index.ids !== 'function') return null;
  const argumentsBefore = call[2].split(',');
  const field = descriptor.filters[argumentsBefore.length - 1];
  let kind = descriptor.predicate.split('.')[0].toLowerCase();
  if (field === 'subject' || field === 'actor') kind = 'npc';
  else if (field === 'destination') kind = 'location';
  else if (field !== 'resource') return [];
  const fragment = argumentsBefore.at(-1).trim();
  const quoted = fragment.startsWith('"');
  const prefix = fragment.replace(/^"/, '');
  return (index.ids(kind) || []).filter(id => id.startsWith(prefix))
    .filter(id => kind !== 'observation' || id.includes('#')).map(id => ({
    label: id, replaceLength: prefix.length, insertText: quoted ? id : `"${id}"`, kind: 'reference',
    detail: `${kind}: ${id}`, sortText: `000_event_resource_${id}`
  }));
}

module.exports = { eventCompletions };
