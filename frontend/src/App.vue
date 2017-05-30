<template>
  <div id="wrapper">
    <nav class="navbar navbar-default">
      <div class="container-fluid">
        <div class="navbar-header">
          <router-link to="/shows"><a class="navbar-brand">TV Shows tracker</a></router-link>
        </div>
        <ul class="nav navbar-nav navbar-right" v-if="hasCookie()">
          <li><a class="clickable-a" @click="openModal">Sign Out</a></li>
        </ul>
        <ul class="nav navbar-nav navbar-right" v-else>
          <li><router-link to="/login">Sign In</router-link></li>
          <li><router-link to="/signup">Sign Up</router-link></li>
        </ul>
      </div>
    </nav>
    <modal id="signout-popup" v-if="showModal" @close="closeModal"></modal>
    <div id="app">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import Modal from './components/ModalSignOut.vue'
Vue.component(Modal.name, Modal)

export default {
  name: 'app',
  data () {
    return {
      showModal: false
    }
  },
  methods: {
    hasCookie () {
      let cookie = this.getCookie('auth_token')
      console.log('cookie: ' + cookie)
      return cookie != null
    },
    getCookie (name) {
      let match = document.cookie.match(new RegExp(name + '=([^;]+)'))
      if (match) return match[1]
      return
    },
    openModal () {
      this.showModal = true
      console.log('opened!', this.showModal)
    },
    closeModal () {
      this.showModal = false
    }
  }
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  /*text-align: center;*/
  color: #2c3e50;
  margin-top: 30px;
}

.clickable-a {
  cursor: pointer;
}

.header-left {
  float: left;
  width: 50%;
}

.header-right {
  float: right;
  width: 50%;
}
</style>
