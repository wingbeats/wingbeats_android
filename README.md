# Wingbeats Android

<p align="center"> <img src="https://github.com/wingbeats/wingbeats_pi/blob/master/wingbeats.png"></p>

Android app for Kaggle dataset [Wingbeats](https://www.kaggle.com/potamitis/wingbeats) 

We present an app that allows wingbeat recordings to be classified on an Android smartphone/tablet. The app comes with pre-installed samples of mosquito wingbeat recordings excluded from the training set and the user can scroll down the list and try out the recognition functionality. The user can also upload recordings from a custom folder of the mobile/tablet. Note that for recordings outside the Wingbeats database an adaptation stage is required. The weights are derived on a desktop computer using Keras 2.1.3 and after being converted to a Tensorflow 1.4 graph are packed as an Android app. The mobile/tablet is only used to predict the correct class of the snippet. The app implementation suggests that a real-time wingbeat recognizer can be directly embedded in tablets and mobiles.

## Prebuilt apk - Fastest path to try the app
Download the prebuilt apk https://github.com/wingbeats/wingbeats_android/raw/master/wingbeats_android.apk and install it in your android device. 

**A folder with the name "Wingbeats_user" will be generated in the External storage in which you can drop your samples, so you can switch between the pre-installed and them inside the app. Note that for recordings outside the Wingbeats database an adaptation stage is required.**

## Upcoming
* Add further details about training and converting Keras model to Tensorflow graph.
* Add android source code.
