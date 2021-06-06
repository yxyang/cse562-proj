from struct import pack, unpack
from math import sin, pi
import wave
import random
import numpy as np
import sounddevice as sd


RATE=44100
total_time_secs = 200
sweep_time_secs = 0.04
sampling_freq = 44100
freq_min_left = 17000
freq_min_right = 22500
bandwidth = 2500



# ## GENERATE MONO FILE ##
# wv = wave.open('mono.wav', 'w')
# wv.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
# maxVol=2**15-1.0 #maximum amplitude
# wvData=""
# for i in range(0, RATE*3):
#     wvData+=pack('h', maxVol*sin(i*500.0/RATE)) #500Hz
# wv.writeframes(wvData)
# wv.close()

## GENERATE STEREO FILE ##
time = np.linspace(0, total_time_secs, total_time_secs * sampling_freq)
time_in_period = np.fmod(time, sweep_time_secs)

wave_left = 10000 * np.sin(2 * np.pi * freq_min_left * time_in_period +
                      np.pi * bandwidth * time_in_period**2 / sweep_time_secs)

wave_right = 10000 * np.sin(2 * np.pi * freq_min_left * time_in_period +
                      np.pi * bandwidth * time_in_period**2 / sweep_time_secs)



wv = wave.open('stereo.wav', 'w')
wv.setparams((2, 2, RATE, 0, 'NONE', 'not compressed'))
maxVol=2**15-1.0 #maximum amplitude
wvData=""

#uncomment this part for testing. (use headphones)
# for i in range(0, RATE*10):
    # wvData+=pack('h', maxVol*sin(i*500.0/RATE)) #500Hz left
    # wvData+=pack('h', 2**10*sin(i*500.0/RATE)) #500Hz left
    #####################
    # wvData+=pack('h', maxVol*sin(i*200.0/RATE)) #200Hz right

for i in range(wave_left.shape[0]):
	wvData+=pack('h', wave_left[i])
	wvData+=pack('h', wave_right[i])

wv.writeframes(wvData)
wv.close()