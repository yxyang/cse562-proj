from struct import pack, unpack
from math import sin, pi
import wave
import random
import numpy as np


RATE=44100
total_time_secs = 100 #200
sweep_time_secs = 0.04
sampling_freq = 44100
freq_min_left = 16000 #17000
freq_min_right = 19000 #20000
bandwidth = 2500

#Generate chirps
time = np.linspace(0, total_time_secs, total_time_secs * sampling_freq)
time_in_period = np.fmod(time, sweep_time_secs)

wave_left = 10000 * np.sin(2 * np.pi * freq_min_left * time_in_period +
                      np.pi * bandwidth * time_in_period**2 / sweep_time_secs)

wave_right = 10000 * np.sin(2 * np.pi * freq_min_right * time_in_period +
                      np.pi * bandwidth * time_in_period**2 / sweep_time_secs)


## GENERATE MONO FILES ##
wv = wave.open('mono_16k.wav', 'w')
wv.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
maxVol=2**15-1.0 #maximum amplitude
wvData=""
for i in range(wave_left.shape[0]):
    wvData+=pack('h',wave_left[i])
wv.writeframes(wvData)
wv.close()


wv = wave.open('mono_19k.wav', 'w')
wv.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
# maxVol=2**15-1.0 #maximum amplitude
wvData=""
for i in range(wave_right.shape[0]):
    wvData+=pack('h',wave_right[i])
wv.writeframes(wvData)
wv.close()


##GENERATE STEREO FILE
wv = wave.open('stereo.wav', 'w')
wv.setparams((2, 2, RATE, 0, 'NONE', 'not compressed'))
# maxVol=2**15-1.0 #maximum amplitude
wvData=""

#uncomment this loop for testing. (use headphones)
# for i in range(0, RATE*10):
    # wvData+=pack('h', maxVol*sin(i*500.0/RATE)) #500Hz left
    # wvData+=pack('h', 2**10*sin(i*500.0/RATE)) #500Hz right (lower volume)
    #####################
    # #### wvData+=pack('h', maxVol*sin(i*200.0/RATE)) #200Hz right

for i in range(wave_left.shape[0]):
	wvData+=pack('h', wave_left[i])
	wvData+=pack('h', wave_right[i])

wv.writeframes(wvData)
wv.close()