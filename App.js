import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';

const { CallDetector, PermissionModule } = NativeModules;

const App = () => {
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [currentCall, setCurrentCall] = useState(null);
  const [riskScore, setRiskScore] = useState(0);

  const requestPermissions = async () => {
    try {
      const result = await PermissionModule.requestPermissions();
      if (!result) {
        Alert.alert(
          'Permissions Required',
          'Please grant permissions from settings to use this feature',
          [
            {
              text: 'Open Settings',
              onPress: () => PermissionModule.openSettings()
            },
            {
              text: 'Cancel',
              style: 'cancel'
            }
          ]
        );
      }
    } catch (error) {
      console.error('Permission error:', error);
      Alert.alert('Error', 'Failed to request permissions');
    }
  };

  useEffect(() => {
    requestPermissions();
  }, []);

  const toggleMonitoring = async () => {
    if (!isMonitoring) {
      await requestPermissions();
    }
    setIsMonitoring(!isMonitoring);
    Alert.alert(
      'Status',
      !isMonitoring ? 'Call monitoring started' : 'Call monitoring stopped'
    );
  };

  // Rest of your component remains the same...
}
