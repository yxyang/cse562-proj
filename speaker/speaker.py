import numpy as np
import sounddevice as sd

sd.default.samplerate = 44100

total_time_secs = 200
sweep_time_secs = 0.04
sampling_freq = 44100
freq_min = 17000
bandwidth = 2500

# Generate time of samples between 0 and two seconds
time = np.linspace(0, total_time_secs, total_time_secs * sampling_freq)
time_in_period = np.fmod(time, sweep_time_secs)

wave = 10000 * np.sin(2 * np.pi * freq_min * time_in_period +
                      np.pi * bandwidth * time_in_period**2 / sweep_time_secs)
# Convert it to wav format (16 bits)
wav_wave = np.array(wave, dtype=np.int16)

sd.play(wav_wave, blocking=True)
