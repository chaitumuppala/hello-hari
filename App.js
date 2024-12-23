import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Linking,
  PermissionsAndroid,
  Platform,
} from 'react-native';

const App = () => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [hasPermission, setHasPermission] = useState(false);

  useEffect(() => {
    checkPermission();
  }, []);

  const checkPermission = async () => {
    try {
      const status = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.CALL_PHONE);
      setHasPermission(status);
    } catch (err) {
      console.warn(err);
    }
  };

  const requestCallPermission = async () => {
    try {
      if (Platform.Version >= 34) { // Android 14 and above
        // First check if we can request the permission
        const granted = await PermissionsAndroid.check(
          PermissionsAndroid.PERMISSIONS.CALL_PHONE
        );
        
        if (!granted) {
          const permissionResult = await PermissionsAndroid.requestMultiple([
            PermissionsAndroid.PERMISSIONS.CALL_PHONE,
            PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
          ]);

          if (permissionResult['android.permission.CALL_PHONE'] === PermissionsAndroid.RESULTS.GRANTED) {
            setHasPermission(true);
            return true;
          }
        }
      } else {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.CALL_PHONE,
          {
            title: 'Call Permission',
            message: 'HelloHari needs access to make phone calls.',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          }
        );

        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          setHasPermission(true);
          return true;
        }
      }
      
      // If permission not granted, show settings dialog
      Alert.alert(
        'Permission Required',
        'Please grant call permission in settings to make calls.',
        [
          {
            text: 'Open Settings',
            onPress: () => {
              Linking.openSettings().catch(() => {
                Alert.alert('Unable to open settings');
              });
            },
          },
          {
            text: 'Cancel',
            style: 'cancel',
          },
        ]
      );
      return false;
    } catch (err) {
      console.warn('Permission request error:', err);
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
      if (!hasPermission) {
        const granted = await requestCallPermission();
        if (!granted) return;
      }
      try {
        await Linking.openURL(`tel:${phoneNumber}`);
      } catch (err) {
        Alert.alert('Error', 'Could not make the call');
      }
    }
  };

  const renderDialPad = () => {
    const numbers = [
      ['1', '2', '3'],
      ['4', '5', '6'],
      ['7', '8', '9'],
      ['*', '0', '#']
    ];

    return numbers.map((row, rowIndex) => (
      <View key={rowIndex} style={styles.row}>
        {row.map(num => (
          <TouchableOpacity
            key={num}
            style={styles.dialButton}
            onPress={() => handleNumberPress(num)}
          >
            <Text style={styles.dialButtonText}>{num}</Text>
          </TouchableOpacity>
        ))}
      </View>
    ));
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.titleHH}>HH</Text>
        <Text style={styles.titleMain}>Hello Hari</Text>
        <Text style={styles.subtitle}>Your Call Safety Companion</Text>
      </View>

      <View style={styles.phoneNumberContainer}>
        <Text style={styles.phoneNumberText}>{phoneNumber}</Text>
      </View>

      <View style={styles.dialPad}>
        {renderDialPad()}
        <View style={styles.row}>
          <TouchableOpacity style={styles.dialButton} onPress={handleDelete}>
            <Text style={styles.dialButtonText}>⌫</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.dialButton, styles.callButton]} 
            onPress={handleCall}
          >
            <Text style={[styles.dialButtonText, styles.callButtonText]}>Call</Text>
          </TouchableOpacity>
        </View>
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
  phoneNumberContainer: {
    padding: 20,
    alignItems: 'center',
    backgroundColor: '#FFF',
    marginVertical: 20,
  },
  phoneNumberText: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#000000',
  },
  dialPad: {
    padding: 20,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 20,
  },
  dialButton: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#FFF',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 2,
  },
  dialButtonText: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#000000',
  },
  callButton: {
    backgroundColor: '#4CAF50',
  },
  callButtonText: {
    color: '#FFFFFF',
  },
});

export default App;
