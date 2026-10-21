"""Original generated sine-wave fixtures; no third-party recordings. Requires ffmpeg path as argv[1]."""
from pathlib import Path
import math
import struct
import subprocess
import sys
import wave

root = Path(__file__).resolve().parents[1] / 'audio-forge-1.20.1/src/test/resources/audio'
root.mkdir(parents=True, exist_ok=True)
wav = root / 'test_tone.wav'
with wave.open(str(wav), 'wb') as stream:
    stream.setparams((1, 2, 22050, 0, 'NONE', 'not compressed'))
    stream.writeframes(b''.join(struct.pack('<h', int(4000 * math.sin(i * 2 * math.pi * 440 / 22050)))
                              for i in range(11025)))
for extension, codec in [('ogg', 'libvorbis'), ('mp3', 'libmp3lame')]:
    subprocess.run([sys.argv[1], '-y', '-loglevel', 'error', '-i', str(wav), '-c:a', codec,
                    str(root / ('test_loop.' + extension))], check=True)
