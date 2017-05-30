<template>
  <div class="signup">
    <form class="form-signin" onsubmit="event.preventDefault()">
      <h2 class="form-signin-heading">Sign up</h2>
      <input type="text" class="form-control" name="username" placeholder="Username" required autofocus v-model="username"/>
      <input type="password" class="form-control" name="password" placeholder="Password" required v-model="password"/>
      <button class="btn btn-lg btn-primary btn-block" v-on:click="signup">Sign Up</button>
    </form>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'signup',
  data: () => ({
    username: '',
    password: ''
  }),
  methods: {
    signup () {
      axios.defaults.withCredentials = true
      axios.post(`api/auth/signUp?username=${this.username}&password=${this.password}`)
      .then(response => {
        console.log(response)
      })
      .catch(error => {
        console.log(error)
      })
    },
    getCookie (name) {
      let match = document.cookie.match(new RegExp(name + '=([^;]+)'))
      if (match) return match[1]
      return
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
body {
  background: #eee !important;  
}

.wrapper {  
  margin-top: 80px;
  margin-bottom: 80px;
}

.form-signin {
  max-width: 380px;
  padding: 15px 35px 45px;
  margin: 0 auto;
  background-color: #fff;
  border: 1px solid rgba(0,0,0,0.1);
}

.form-signin-heading {
  margin-bottom: 30px;
}

.form-control {
  position: relative;
  font-size: 16px;
  height: auto;
  padding: 10px;
  box-sizing: border-box;

  &:focus {
    z-index: 2;
  }
}

input[type=text] {
  margin-bottom: -1px;
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

input[type=password] {
  margin-bottom: 20px;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}

</style>
