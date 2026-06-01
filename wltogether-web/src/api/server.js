import api from './index'

export default {
  getCapabilities() {
    return api.get('/api/server/capabilities')
  }
}
