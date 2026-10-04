#!/usr/bin/env python3
"""Reproduce #6 against the pinned external CLI; no plugin runtime dependency."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

executable = os.environ.get('QUINT_TEST_EXECUTABLE')
if not executable or not Path(executable).is_absolute():
    raise SystemExit('Set QUINT_TEST_EXECUTABLE to an absolute Quint executable')
version = dict(line.split('=', 1) for line in Path('ci/quint-test-toolchain.properties').read_text().splitlines() if '=' in line)['quintVersion']
assert subprocess.check_output([executable, '--version'], text=True).strip() == version
source = '''module main {
  type R = { child: { count: int } }
  pure def make(n: int): R = { child: { count: n } }
  pure val r = make(1)
  pure val x = make(2).child.count
}
'''
summary = []
with tempfile.TemporaryDirectory(prefix='quint-expression-types-') as directory:
    base = Path(directory)
    for variant, contents in [('plain', source), ('unicode_crlf', source.replace('  pure val x', '  /* 😀 */ pure val x').replace('\n', '\r\n')), ('incomplete', source.replace('make(2).child.count', 'make(2).'))]:
        path = base / 'main.qnt'
        path.write_bytes(contents.encode())
        parsed, checked, source_map = [base / name for name in ('parse.json', 'check.json', 'map.json')]
        parse = subprocess.run([executable, 'parse', str(path), '--out', str(parsed), '--source-map', str(source_map)], capture_output=True)
        check = subprocess.run([executable, 'typecheck', str(path), '--out', str(checked)], capture_output=True)
        p, c = json.loads(parsed.read_text()), json.loads(checked.read_text())
        if variant == 'incomplete':
            assert parse.returncode != 0 and check.returncode != 0
            summary.append({'variant': variant, 'mapping': 'unavailable: syntax errors'})
            continue
        assert parse.returncode == check.returncode == 0, c.get('errors')
        assert p['modules'] == c['modules'], 'Separate commands must retain the same IR identities'
        mapping = json.loads(source_map.read_text())['map']
        text = contents
        expressions = {}
        def visit(value):
            if isinstance(value, dict):
                if value.get('kind') == 'app':
                    expressions[str(value['id'])] = value
                for child in value.values():
                    visit(child)
            elif isinstance(value, list):
                for child in value:
                    visit(child)
        visit(p['modules'])
        found = []
        for fragment, kind in [('make(2)', 'rec'), ('make(2).child', 'rec'), ('make(2).child.count', 'int')]:
            matches = []
            for identity in expressions:
                location = mapping.get(identity)
                if location is None:
                    continue
                start, end = location[1]['index'], location[2]['index'] + 1
                if text[start:end] == fragment:
                    inferred = c['types'][identity]['type']['kind']
                    assert inferred == kind, (variant, fragment, inferred)
                    matches.append(identity)
            assert len(matches) == 1, (variant, fragment, matches)
            found.append({'expression': fragment, 'kind': kind})
        summary.append({'variant': variant, 'same_ir': True, 'index_units': 'Unicode code points in root input (including CRLF)', 'expressions': found})
print(json.dumps({'quint': version, 'decision': 'go for valid immutable snapshots with IR-based disambiguation; no mapping for incomplete source', 'cases': summary}, indent=2))
