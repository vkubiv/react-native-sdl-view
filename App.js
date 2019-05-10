/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {StyleSheet, Text, View, Button} from 'react-native';
import SDLView from './sdl-view';

type Props = {};
export default class App extends Component<Props> {

  state = {
    showView: false
  };

  onButtonPress = () => {
    this.setState({
      showView: true
    });
  };

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>Welcome to React Native!</Text>
        <Text style={styles.instructions}>Here SDL view will be shown:</Text>
        <View style={styles.sdlPlaceHolder}>
          {this.state.showView && <SDLView style={styles.sdl} />}
        </View>

        <Button onPress={this.onButtonPress} title="Show SDL view" />

      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },

  sdlPlaceHolder: {
    width: 250,
    height: 250,
  },

  sdl: {
    width: 250,
    height: 250,
  },

  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
