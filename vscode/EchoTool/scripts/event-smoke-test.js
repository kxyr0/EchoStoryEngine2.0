'use strict';
const assert = require('node:assert/strict');
const { eventCompletions } = require('../src/language/eventCompletions');
const kinds = [];
const index = { ids(kind) { kinds.push(kind); return ['jdh:father', 'jdh:mother']; } };
assert.deepEqual(eventCompletions('When(Dialogue.', index).map(value => value.label).sort(),
  ['Cancelled', 'Choice', 'Completed', 'Failed', 'Started']);
assert.equal(eventCompletions('Dialogue.', index), null);
assert.equal(eventCompletions('When(Dialogue.Completed("jdh:fa', index)[0].insertText, 'jdh:father');
assert.equal(kinds.at(-1), 'dialogue');
assert.equal(eventCompletions('When(Animation.Marker("jdh:father", ', index).length, 2);
assert.equal(kinds.at(-1), 'animation');
assert.equal(eventCompletions('When(Npc.Arrived("jdh:father", ', index).length, 2);
assert.equal(kinds.at(-1), 'location');
assert.equal(eventCompletions('When(Dialogue.Choice("jdh:father", ', index).length, 0);
console.log('EE1 completion checks passed');

const { eventDiagnostics } = require('../src/language/eventDiagnostics');
assert.equal(eventDiagnostics('When(Dialogue.Completed("jdh:father"))', index).length, 0);
assert.equal(eventDiagnostics('When(Dialogue.Wrong("jdh:father"))', index)[0].line, 0);
assert.equal(eventDiagnostics('When(Dialogue.Choice("jdh:father"))', index).length, 1);
assert.equal(eventDiagnostics('// When(Dialogue.Wrong("jdh:father"))', index).length, 0);
assert.equal(eventDiagnostics('/* When(Dialogue.Wrong("jdh:father")) */', index).length, 0);
assert.equal(eventDiagnostics('When(Dialogue.Completed("jdh:missing"))', index).length, 1);

assert.equal(eventDiagnostics('When(Dialogue.Wrong("jdh:father"), id: "after_father") { Scene.Play("jdh:scene"); }', index).length, 1);
assert.equal(eventDiagnostics('When(Dialogue.Completed("jdh:father"), id: "after_father", once: true) {}', index).length, 0);
