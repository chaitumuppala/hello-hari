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
  PermissionsAndroid,
} from 'react-native';

const { CallDetector } = NativeModules;

const App = () => {
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [currentCall, setCurrentCall] = useState(null);
  const [riskScore, setRiskScore] = useState(0);

  const requestPermission = async () => {
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
        {
          title: 'Phone Permission',
          message: 'Hello Hari needs access to your phone.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }
      );
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        Alert.alert('Success', 'Phone permission granted');
      } else {
        Alert.alert('Permission denied');
      }
    } catch (err) {
      Alert.alert('Error', err.toString());
    }
  };

  const toggleMonitoring = () => {
    if (!isMonitoring) {
      requestPermission();
    }
    setIsMonitoring(!isMonitoring);
    Alert.alert(
      'Status',
      !isMonitoring ? 'Call monitoring started' : 'Call monitoring stopped'
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.titleHH}>HH</Text>
        <Text style={styles.titleMain}>Hello Hari</Text>
        <Text style={styles.subtitle}>Your Call Safety Companion</Text>
      </View>

      <TouchableOpacity 
        style={[styles.monitorButton, isMonitoring && styles.monitoringActive]}
        onPress={toggleMonitoring}
      >
        <Text style={styles.buttonText}>
          {isMonitoring ? 'Stop Monitoring' : 'Start Monitoring'}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={styles.permissionButton}
        onPress={requestPermission}
      >
        <Text style={styles.buttonText}>
          Request Permission
        </Text>
      </TouchableOpacity>

      {currentCall && (
        <View style={styles.callCard}>
          <Text style={styles.callNumber}>Number: {currentCall.number}</Text>
          <Text style={styles.riskLevel}>Risk Level: {currentCall.riskLevel}</Text>
        </View>
      )}

      <View style={styles.riskMeter}>
        <Text style={styles.riskTitle}>Current Risk Level</Text>
        <View style={styles.riskBar}>
          <View style={[styles.riskFill, { width: `${riskScore}%` }]} />
        </View>
        <Text style={styles.riskPercentage}>{riskScore}%</Text>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F6FA',
  },
  header: {
    padding: 20,
    backgroundColor: '#2E3192',
    alignItems: 'center',
  },
  titleHH: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#FFF',
    marginBottom: 4,
  },
  titleMain: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#FFF',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 14,
    color: '#E0E0E0',
  },
  monitorButton: {
    margin: 16,
    padding: 16,
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    alignItems: 'center',
  },
  permissionButton: {
    margin: 16,
    padding: 16,
    backgroundColor: '#2196F3',
    borderRadius: 8,
    alignItems: 'center',
  },
  monitoringActive: {
    backgroundColor: '#F44336',
  },
  buttonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  callCard: {
    margin: 16,
    padding: 16,
    backgroundColor: '#FFF',
    borderRadius: 8,
    elevation: 1,
  },
  callNumber: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  riskLevel: {
    marginTop: 8,
    color: '#666',
  },
  riskMeter: {
    margin: 16,
    padding: 16,
    backgroundColor: '#FFF',
    borderRadius: 8,
    elevation: 1,
  },
  riskTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  riskBar: {
    height: 8,
    backgroundColor: '#EEE',
    borderRadius: 4,
  },
  riskFill: {
    height: '100%',
    backgroundColor: '#4CAF50',
    borderRadius: 4,
  },
  riskPercentage: {
    marginTop: 8,
    textAlign: 'right',
  },
});

export default App;
