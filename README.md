# Wingbeats Android

<p align="center"> <img src="https://github.com/wingbeats/wingbeats_pi/blob/master/wingbeats.png"></p>

Android app for Kaggle dataset [Wingbeats](https://www.kaggle.com/potamitis/wingbeats) 

We present an app that allows wingbeat recordings to be classified on an Android smartphone/tablet. The app comes with pre-installed folders of mosquito wingbeat recordings excluded from the training set and the user can scroll down the list and try out the recognition functionality. The user can also upload recordings from the SD card or Downloads folder of the mobile/tablet. Note that for recordings outside the Wingbeats database an adaptation stage is required. The weights are derived on a desktop computer using Keras 2.1.3 and after being converted to a Tensorflow 1.4 graph are packed as an Android app. The mobile/tablet is only used to predict the correct class of the snippet. The app implementation suggests that a real-time wingbeat recognizer can be directly embedded in tablets and mobiles.
