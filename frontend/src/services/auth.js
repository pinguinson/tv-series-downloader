import router from '../router'
import axios from 'axios'

export default {
  authenticated: false,
  
  login (context, username, password, redirect) {
    axios.post(`api/auth/signIn?username=${username}&password=${password}`)
    .then(response => {
      console.log(response)
      this.authenticated = true

      if (redirect) {
        router.go(redirect)
      }
    }).catch(error => {
      console.log(error)
    })
  },

  logout () {
    this.authenticated = false
    router.go('/login')
  }
}
