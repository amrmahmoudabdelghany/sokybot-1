import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const cwd = process.cwd();
const javaPhaseFile = path.resolve(
  cwd,
  '../../../../../game/sokybot-game-model/src/main/java/org/sokybot/gamemodel/LoginState.java',
);
const mappingFile = path.resolve(cwd, './src/machines/loginPhaseMapping.ts');
const allowlistFile = path.resolve(cwd, './config/login-phase-fallback-allowlist.json');
const artifactPath = path.resolve(cwd, './target/generated/login-phase-contract.json');

function parseJavaPhases(content) {
  const enumStart = content.indexOf('enum Phase');
  if (enumStart < 0) throw new Error('Could not find LoginState.Phase enum');
  const open = content.indexOf('{', enumStart);
  const close = content.indexOf('}', open);
  const body = content.slice(open + 1, close);
  return body
    .split(',')
    .map((s) => s.trim())
    .filter((s) => /^[A-Z0-9_]+$/.test(s));
}

function parseMappedPhases(content) {
  const mapStart = content.indexOf('const PHASE_MAP');
  if (mapStart < 0) throw new Error('Could not find PHASE_MAP in loginPhaseMapping.ts');
  const open = content.indexOf('{', mapStart);
  let depth = 0;
  let close = -1;
  for (let i = open; i < content.length; i++) {
    const ch = content[i];
    if (ch === '{') depth++;
    if (ch === '}') {
      depth--;
      if (depth === 0) {
        close = i;
        break;
      }
    }
  }
  const body = content.slice(open + 1, close);
  const matches = body.match(/^\s*([A-Z0-9_]+)\s*:\s*\{/gm) || [];
  return matches.map((m) => m.replace(/[:{\s]/g, ''));
}

async function main() {
  const [javaSource, mappingSource, allowlistRaw] = await Promise.all([
    readFile(javaPhaseFile, 'utf8'),
    readFile(mappingFile, 'utf8'),
    readFile(allowlistFile, 'utf8'),
  ]);
  const backendPhases = parseJavaPhases(javaSource);
  const mappedPhases = parseMappedPhases(mappingSource);
  const allowlist = JSON.parse(allowlistRaw).allowedUnmappedPhases || [];

  const missing = backendPhases.filter(
    (phase) => !mappedPhases.includes(phase) && !allowlist.includes(phase),
  );

  await mkdir(path.dirname(artifactPath), { recursive: true });
  await writeFile(
    artifactPath,
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        backendPhases,
        mappedPhases,
        allowlist,
        missing,
      },
      null,
      2,
    ),
    'utf8',
  );

  if (missing.length > 0) {
    console.error(
      `Phase contract check failed. Missing mappings: ${missing.join(', ')}.\n` +
        `Either add mappings in loginPhaseMapping.ts or allow explicit fallback in config/login-phase-fallback-allowlist.json`,
    );
    process.exit(1);
  }

  console.log(
    `Phase contract check passed (${backendPhases.length} backend phases, ${mappedPhases.length} mapped, ${allowlist.length} allowlisted).`,
  );
}

main().catch((err) => {
  console.error('Phase contract check failed:', err);
  process.exit(1);
});
