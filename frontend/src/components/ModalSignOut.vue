<template>
  <transition name="modal">
    <div class="modal-mask">
      <div class="modal-wrapper modal-dialog">
        <div class="modal-container">
          <p class="modal-header">Sign out?</p>
          <div class="btn-group btn-group-justified">
            <a class="btn btn-default" @click="signOut">Yes</a>
            <a class="btn btn-default" @click="$emit('close')">No</a>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
import axios from 'axios'

export default {
  name: 'modal',
  methods: {
    signOut () {
      axios.defaults.withCredentials = true
      axios.post('api/auth/signOut')
      .then(response => {
        console.log(response)
        this.$router.go('/login')
      })
      .catch(error => {
        console.log(error)
      })
    }
  }
}
</script>

<style>
.modal-mask {
  position: fixed;
  z-index: 9998;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, .5);
  display: table;
  transition: opacity .6s ease;
}

.modal-wrapper {
  display: table-cell;
  vertical-align: middle;
}

.modal-container {
  width: 300px;
  margin: 0px auto;
  padding: 20px 30px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .33);
  transition: all .3s ease;
  font-family: Helvetica, Arial, sans-serif;
}

.modal-header {
  display: block;
  font-weight: 600;
  color: #000;
  text-align: left;
  border-bottom: none;
}

.modal-body {
  margin: 20px 0;
}

.modal-default-button {
  float: right;
}

.modal-enter {
  opacity: 0;
}

.modal-leave-active {
  opacity: 0;
}

.modal-enter .modal-container,
.modal-leave-active .modal-container {
  -webkit-transform: scale(1.1);
  transform: scale(1.1);
}
</style>
