<template>
  <v-card>
    <v-card-title>
      <v-btn color="info">新增</v-btn>
      <v-spacer></v-spacer>
      <v-text-field
        v-model="search"
        append-icon="search"
        label="Search"
        single-line
        hide-details
      ></v-text-field>
    </v-card-title>
    <v-data-table
      :headers="headers"
      :items="brands"
      :search="search"
    >
      <template slot="items" slot-scope="props">
        <td class="text-xs-center">{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.name }}</td>
        <td class="text-xs-center"><img :src="props.item.image"/></td>
        <td class="text-xs-center">{{ props.item.letter }}</td>
        <td class="justify-center layout px-0">
          <v-icon
            small
            class="mr-2 mt-3"
            @click="editItem(props.item)"
          >
            edit
          </v-icon>
          <v-icon
            class="mr-2 mt-3"
            small
            @click="deleteItem(props.item)"
          >
            delete
          </v-icon>
        </td>
      </template>
      <v-alert slot="no-results" :value="true" color="error" icon="warning">
        Your search for "{{ search }}" found no results.
      </v-alert>
    </v-data-table>
  </v-card>
</template>

<script>

  import {brandData} from "../../mockDB"

  export default {
    name: "MyBrand",
    data() {
      return {
        search: '',
        headers: [
          {
            text: 'id',
            align: 'center',
            value: 'id'
          },
          {text: 'name', align: 'center', value: 'name', sortable: false},
          {text: 'Logo', align: 'center', value: 'image', sortable: false},
          {text: '首字母', value: 'letter', align: 'center'},
          {text: '操作', value: 'id', align: 'center', sortable: false}
        ],
        brands:brandData
      }
    },
    created(){

    }



  }
</script>

<style scoped>

</style>
