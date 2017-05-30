<template>
  <div class="shows">
    <ul class="list-group">
    <li v-for="show of shows" class="list-group-item show-item">
      <div class="float-left">
        <span class="show-title">{{ show.imdbId }}</span>
        <span class="show-info">Starting with {{ episodeToString(show) }}</span>
      </div>
      <div class="float-right">
        <button type="button" class="btn btn-sm btn-danger fixed-width-button" v-on:click="unsubscribe(show.imdbId)">Unsubscribe</button>
      </div>
    </li>
    <li class="list-group-item show-item">
      <form class="form-inline" onsubmit="event.preventDefault()">
        <div class="float-left">
          <div class="form-group">
            <label for="imdbId">IMDb id:</label>
            <input type="text" class="form-control" size="12" id="imdbId" v-model="imdbId">
          </div>
          <div class="form-group">
            <label for="season">Season:</label>
            <input type="text" class="form-control" size="2" id="season" v-model="season">
          </div>
          <div class="form-group">
            <label for="episode">Episode:</label>
            <input type="text" class="form-control" size="2" id="episode" v-model="episode">
          </div>
        </div>
        <div class="float-right">
          <button type="button" class="btn btn-sm btn-success fixed-width-button" v-on:click="subscribe(imdbId, season, episode)">Subscribe</button>
        </div>
      </form>
    </li>
  </ul>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'shows',
  data: () => ({
    shows: [],
    imdbId: '',
    season: '',
    episode: ''
  }),
  created () {
    this.getShows()
  },
  methods: {
    getShows () {
      axios.defaults.withCredentials = true
      axios.get('api/user/shows')
      .then(response => {
        this.shows = response.data['shows']
      })
      .catch(error => {
        console.log(error)
      })
    },
    pad (n) {
      if (n < 10) {
        return '0' + n
      } else {
        return n
      }
    },
    unsubscribe (imdbId) {
      axios.post(`api/user/unsubscribe?imdbId=${imdbId}`)
      .then(this.getShows())
      .catch(error => {
        console.log(error)
      })
    },
    subscribe (imdbId, season, episode) {
      axios.post(`api/user/subscribe?imdbId=${imdbId}&season=${season}&episode=${episode}`)
      .then(response => {
        this.getShows()
        setTimeout(this.getShows(), 60000)
      })
      .catch(error => {
        console.log(error)
      })
    },
    episodeToString (show) {
      return `s${this.pad(show.startWithSeason)}e${this.pad(show.startWithEpisode)}`
    }
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h1 {
  font-weight: normal;
}

p {
  display: inline;
}

li {
  /*margin-bottom: 5pt;*/
}

button {
  display: inline;
}

button.fixed-width-button {
    width: 100px !important;
}

.float-left {
  float: left;
}

.float-right {
  float: right;
}

.shows {
  width: 750px;
  float: left;
  margin-left: 20px;
}

.show-item {
  height: 56px;
}

.show-title {
  display: block;
  font-weight: 600;
  color: #000;
  text-align: left;
}

.show-info {
  display: inline-block;
  font-size: 12px !important;
  color: #586069 !important;
  text-align: left;
}

</style>
