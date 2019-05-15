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
    showView: false,
    textureSrc: 'texture2.bmp',
  };

  onShowViewPress = () => {
    this.setState({
      showView: !this.state.showView
    });
  };

  onChangeTexturePress = () => {
    this.setState({
      textureSrc: 'texture1.bmp',
    });
  };

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>Welcome to React Native!</Text>
        <Text style={styles.instructions}>Here SDL view will be shown:</Text>
        <View style={styles.sdlPlaceHolder}>
          {this.state.showView && <SDLView style={styles.sdl} textureSrc={this.state.textureSrc} />}
        </View>

        <Button onPress={this.onShowViewPress} title={!this.state.showView ? 'Show SDL view' : 'Hide SDL view'} />
        <View style={styles.changeBtn}>
          {this.state.showView && <Button  onPress={this.onChangeTexturePress} title={'Change texture'} /> }
        </View>

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
    marginTop: 10,
    marginBottom: 10,
  },

  sdl: {
    width: 250,
    height: 250,
  },

  changeBtn: {
    padding: 10,
  },

  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
