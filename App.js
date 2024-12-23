import React, { useState } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Linking,
  PermissionsAndroid,
} from 'react-native';

const App = () => {
  const [phoneNumber, setPhoneNumber] = useState('');

  const checkAndRequestCallPermission = async () => {
    try {
      // First check if we have permission
      const hasPermission = await PermissionsAndroid.check(
        PermissionsAndroid.PERMISSIONS.CALL_PHONE
      );

      if (hasPermission) {
        return true;
      }

      // Should we show a rationale?
      const shouldShowRationale = await PermissionsAndroid.shouldShowRequestPermissionRationale(
        PermissionsAndroid.PERMISSIONS.CALL_PHONE
      );

      if (shouldShowRationale) {
        // Show rationale if needed
        return new Promise((resolve) => {
          Alert.alert(
            'Phone Permission Required',
            'Hello Hari needs permission to make phone calls. This allows you to make calls directly from the app.',
            [
              {
                text: 'Cancel',
                onPress: () => resolve(false),
                style: 'cancel',
              },
              {
                text: 'OK',
                onPress: async () => {
                  const granted = await requestCallPermission();
                  resolve(granted);
                },
              },
            ]
          );
        });
      }

      // If no rationale needed, request directly
      return await requestCallPermission();
    } catch (err) {
      console.warn(err);
      return false;
    }
  };

  const requestCallPermission = async () => {
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.CALL_PHONE,
        {
          title: 'Call Permission',
          message: 'Hello Hari needs permission to make phone calls.',
          buttonPositive: 'OK',
          buttonNegative: 'Cancel',
        }
      );
      return granted === PermissionsAndroid.RESULTS.GRANTED;
    } catch (err) {
      console.warn(err);
      return false;
    }
  };

  const handleNumberPress = (num) => {
    setPhoneNumber(prevNumber => prevNumber + num);
  };

  const handleDelete = () => {
    setPhoneNumber(prevNumber => prevNumber.slice(0, -1));
  };

  const handleCall = async () => {
    if (phoneNumber.length > 0) {
      const hasPermission = await checkAndRequestCallPermission();
      if (hasPermission) {
        // Make the call
        Linking.openURL(`tel:${phoneNumber}`);
      } else {
        // Degrade gracefully
        Alert.alert(
          'Permission Denied',
          'You can still copy the number and dial manually.',
          [
            {
              text: 'Copy Number',
              onPress: () => {
                Clipboard.setString(phoneNumber);
                Alert.alert('Number Copied', 'You can now dial manually');
              },
            },
            {
              text: 'Cancel',
              style: 'cancel',
            },
          ]
        );
      }
    }
  };

  // Rest of the component remains the same...
