"""Fail closed if game data, patched engine classes or personal data enter the installer."""
import sys, zipfile, io
from pathlib import PurePosixPath
with zipfile.ZipFile(sys.argv[1]) as payload:
    names=payload.namelist()
    assert all(not PurePosixPath(n).is_absolute() and '..' not in PurePosixPath(n).parts for n in names)
    required={'DelverVR.exe','DelverVR-Uninstall.exe','mod.jar','supported-game.sha256','LICENSE','THIRD-PARTY.md'}
    assert required.issubset(names), required-set(names)
    assert not any(n.endswith(('.apk','.dex','.log','.trace')) or 'delver.jar' in n.lower() or n=='patch.jar' for n in names)
    for n in names:
        assert n.startswith(('lib/','licenses/')) or n in required|{'README.md','README.uk.md'},n
    with zipfile.ZipFile(io.BytesIO(payload.read('mod.jar'))) as mod:
        classes=[n for n in mod.namelist() if n.endswith('.class')]
        assert classes and all(n.startswith('delver/vr/') for n in classes), 'Game classes were included'
        assert 'delver/vr/PatchGame.class' in classes
    for name in ['licenses/Apache-2.0.txt','licenses/LWJGL.txt','licenses/OpenVR.txt','licenses/OpenAL-Soft.txt','licenses/openal-soft-1.23.1-source.zip']:
        assert name in names,name
print('PUBLIC PAYLOAD PASS: own mod classes, local patcher, licensed dependencies; no game archive/assets or prepatched game classes')
