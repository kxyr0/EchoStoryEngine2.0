'use strict';

const vscode = require('vscode');
const { echoScriptSignatureHelp } = require('./echoscriptSignatureHelp');

class EchoScriptSignatureHelpProvider {
  provideSignatureHelp(document, position) {
    const model = echoScriptSignatureHelp(document.getText(), document.offsetAt(position));
    if (!model) {
      return undefined;
    }

    const help = new vscode.SignatureHelp();
    help.activeSignature = model.activeSignature;
    help.activeParameter = model.activeParameter;
    help.signatures = model.signatures.map(toSignatureInformation);
    return help;
  }
}

function toSignatureInformation(signature) {
  const item = new vscode.SignatureInformation(signature.label, signature.documentation);
  item.parameters = signature.parameters.map(parameter => (
    new vscode.ParameterInformation(parameter.label, parameter.documentation)
  ));
  return item;
}

module.exports = {
  EchoScriptSignatureHelpProvider
};
