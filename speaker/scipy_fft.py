#!/usr/bin/env python
import numpy as np
import matplotlib.pyplot as plt
from scipy.fftpack import fft
from scipy.io import wavfile # get the api
fs, data = wavfile.read('stereo.wav') # load the data
channel_1 = data.T[0] # this is a two channel soundtrack, I get the first track
# channel_1=[(ele/2**8.)*2-1 for ele in channel_1] # this is 8-bit track, b is now normalized on [-1,1)
fft_1 = fft(channel_1) # calculate fourier transform (complex numbers list)

channel_2 = data.T[1] # this is a two channel soundtrack, I get the first track
# channel_2=[(ele/2**8.)*2-1 for ele in channel_2] # this is 8-bit track, b is now normalized on [-1,1)
fft_2 = fft(channel_2) # calculate fourier transform (complex numbers list)

d1 = len(fft_1) // 2  # you only need half of the fft list (real signal symmetry)
k1 = np.arange(len(data))
T1 = len(data)/fs  # where fs is the sampling frequency
frqLabel1 = k1/T1 

d2 = len(fft_1) // 2  # you only need half of the fft list (real signal symmetry)
k2 = np.arange(len(data))
T2 = len(data)/fs  # where fs is the sampling frequency
frqLabel2 = k2/T2 

fig, ax = plt.subplots(2,1)
ax[0].plot(frqLabel1[:d1-1], abs(fft_1[:d1-1]),'r',)
ax[1].plot(frqLabel2[:d2-1], abs(fft_2[:d2-1]),'r',)
ax[0].set_title('Channel 1 FFT')
ax[1].set_title('Channel 2 FFT') 

plt.show()

