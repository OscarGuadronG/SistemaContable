<template>
  <q-page class="pagina q-pa-md">
    <encabezado-pagina
      titulo="Inventario"
      subtitulo="Productos, existencias por almacén y ajustes autorizados"
      icono="inventory_2"
    >
      <template #acciones>
        <q-btn
          v-if="puedeProductos"
          color="primary"
          unelevated
          icon="add"
          label="Nuevo producto"
          @click="abrirProducto()"
        />
      </template>
    </encabezado-pagina>

    <q-card flat bordered>
      <q-card-section>
        <div class="row q-col-gutter-sm items-end">
          <div class="col-12 col-md-4">
            <q-input
              v-model="filtros.buscar"
              outlined
              dense
              clearable
              label="Nombre o categoría"
              @keyup.enter="reiniciar"
            >
              <template #prepend><q-icon name="search" /></template>
            </q-input>
          </div>
          <div class="col-12 col-sm-4 col-md-2">
            <q-select
              v-model="filtros.tipoId"
              :options="tipos"
              emit-value
              map-options
              clearable
              outlined
              dense
              label="Tipo"
              option-value="id"
              :option-label="etiquetaTipo"
            />
          </div>
          <div class="col-6 col-sm-4 col-md-2">
            <q-select
              v-model="filtros.disponibilidad"
              :options="disponibilidades"
              emit-value
              map-options
              outlined
              dense
              label="Disponibilidad"
            />
          </div>
          <div class="col-6 col-sm-4 col-md-2">
            <q-select
              v-model="filtros.almacenId"
              :options="almacenes"
              emit-value
              map-options
              clearable
              outlined
              dense
              label="Almacén"
              option-value="id"
              option-label="nombre"
            />
          </div>
          <div class="col-auto">
            <q-btn color="primary" outline label="Buscar" @click="reiniciar" />
          </div>
        </div>
      </q-card-section>
      <q-banner v-if="error" class="bg-red-1 text-negative q-mx-md" rounded>
        {{ error }}
        <template #action>
          <q-btn flat color="negative" label="Reintentar" @click="reiniciar" />
        </template>
      </q-banner>
      <q-table
        flat
        class="tabla-app"
        :rows="filas"
        :columns="columnas"
        row-key="productoId"
        v-model:pagination="paginacion"
        :loading="cargando"
        :rows-per-page-options="[10, 20, 50, 100]"
        @request="cargar"
      >
        <template #body-cell-existenciaTotal="props">
          <q-td :props="props" class="text-right monto">
            {{ cantidad(props.row.existenciaTotal) }}
          </q-td>
        </template>
        <template #body-cell-costoPromedio="props">
          <q-td :props="props" class="text-right monto">
            {{ moneda(props.row.costoPromedio) }}
          </q-td>
        </template>
        <template #body-cell-acciones="props">
          <q-td :props="props">
            <q-btn
              flat
              dense
              color="primary"
              label="Detalle"
              @click="verDetalle(props.row)"
            />
            <q-btn
              v-if="puedeProductos"
              flat
              dense
              label="Editar"
              @click="abrirProducto(props.row)"
            />
          </q-td>
        </template>
        <template #no-data>
          <div class="full-width text-center q-pa-lg text-grey-7">
            <q-icon name="inventory_2" size="36px" />
            <div>No hay productos con esos filtros.</div>
          </div>
        </template>
      </q-table>
    </q-card>

    <q-dialog v-model="detalleAbierto">
      <q-card style="width: min(920px, 96vw)">
        <q-card-section class="row items-center">
          <div>
            <div class="text-h6">{{ detalle?.nombre }}</div>
            <div class="text-caption text-grey-7">{{
              detalle?.descripcion
            }}</div>
          </div>
          <q-space />
          <q-btn
            v-if="puedeAjustar"
            color="primary"
            outline
            label="Ajustar existencias"
            @click="abrirAjuste"
          />
          <q-btn flat round icon="close" v-close-popup />
        </q-card-section>
        <q-separator />
        <q-card-section>
          <div v-if="cargandoDetalle" class="text-center q-pa-lg">
            <q-spinner color="primary" size="32px" />
          </div>
          <template v-else-if="detalle">
            <div class="text-subtitle2 q-mb-sm">Por almacén</div>
            <q-markup-table flat bordered dense class="q-mb-md tabla-app">
              <thead>
                <tr>
                  <th class="text-left">Almacén</th>
                  <th class="text-right">Cantidad</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="fila in detalle.porAlmacen || []"
                  :key="fila.almacenId"
                >
                  <td>{{ fila.almacen }}</td>
                  <td class="text-right monto">{{
                    cantidad(fila.cantidad)
                  }}</td>
                </tr>
                <tr v-if="!(detalle.porAlmacen || []).length">
                  <td colspan="2" class="text-grey"
                    >Sin existencias por almacén</td
                  >
                </tr>
              </tbody>
            </q-markup-table>
            <div class="text-subtitle2 q-mb-sm">Lotes</div>
            <q-markup-table flat bordered dense class="tabla-app">
              <thead>
                <tr>
                  <th class="text-left">Lote</th>
                  <th class="text-left">Caducidad</th>
                  <th class="text-left">Almacén</th>
                  <th class="text-right">Cantidad</th>
                  <th class="text-right">Costo</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="lote in detalle.lotes || []"
                  :key="`${lote.loteId}-${lote.almacenId}`"
                >
                  <td>{{ lote.numLote }}</td>
                  <td>{{ fecha(lote.fechCaducidad) }}</td>
                  <td>{{ lote.almacen }}</td>
                  <td class="text-right monto">{{
                    cantidad(lote.cantidad)
                  }}</td>
                  <td class="text-right monto">{{
                    moneda(lote.costoUnitario)
                  }}</td>
                </tr>
                <tr v-if="!(detalle.lotes || []).length">
                  <td colspan="5" class="text-grey"
                    >Sin lotes con existencia</td
                  >
                </tr>
              </tbody>
            </q-markup-table>
          </template>
        </q-card-section>
      </q-card>
    </q-dialog>

    <q-dialog v-model="productoAbierto" persistent>
      <q-card style="width: min(640px, 96vw)">
        <q-card-section class="text-h6">
          {{ productoId ? 'Editar producto' : 'Nuevo producto' }}
        </q-card-section>
        <q-form @submit.prevent="guardarProducto">
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="producto.nombre"
              outlined
              dense
              label="Nombre"
              :rules="[requerido('Indique el nombre')]"
            />
            <q-input
              v-model="producto.descripcion"
              outlined
              dense
              autogrow
              label="Descripción"
            />
            <div class="row q-col-gutter-sm">
              <div class="col">
                <selector-busqueda
                  v-model="producto.unidadMedidaId"
                  :opciones="unidades"
                  etiqueta="Unidad de medida"
                  :opcion-etiqueta="u => `${u.nombre} (${u.abrev})`"
                  :rules="[requerido('Seleccione la unidad')]"
                />
              </div>
              <div class="col-auto">
                <q-btn
                  flat
                  color="primary"
                  label="Nueva"
                  @click="unidadAbierta = true"
                />
              </div>
            </div>
            <div class="row q-col-gutter-sm">
              <div class="col">
                <selector-busqueda
                  v-model="producto.tipoId"
                  :opciones="tipos"
                  etiqueta="Tipo de producto"
                  :opcion-etiqueta="etiquetaTipo"
                  :rules="[requerido('Seleccione el tipo')]"
                />
              </div>
              <div class="col-auto">
                <q-btn
                  flat
                  color="primary"
                  label="Nuevo"
                  @click="tipoAbierto = true"
                />
              </div>
            </div>
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup :disable="guardando" />
            <q-btn
              color="primary"
              unelevated
              type="submit"
              label="Guardar"
              :loading="guardando"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>

    <q-dialog v-model="unidadAbierta" persistent>
      <q-card style="width: min(420px, 94vw)">
        <q-card-section class="text-h6">Nueva unidad de medida</q-card-section>
        <q-form @submit.prevent="crearUnidad">
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="unidad.nombre"
              outlined
              dense
              label="Nombre"
              :rules="[requerido('Indique el nombre')]"
            />
            <q-input
              v-model="unidad.abrev"
              outlined
              dense
              label="Abreviatura"
              :rules="[requerido('Indique la abreviatura')]"
            />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn
              color="primary"
              unelevated
              type="submit"
              label="Crear"
              :loading="guardando"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>

    <q-dialog v-model="tipoAbierto" persistent>
      <q-card style="width: min(420px, 94vw)">
        <q-card-section class="text-h6">Nuevo tipo de producto</q-card-section>
        <q-form @submit.prevent="crearTipo">
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="tipo.nombre"
              outlined
              dense
              label="Nombre"
              :rules="[requerido('Indique el nombre')]"
            />
            <selector-busqueda
              v-model="tipo.padreId"
              :opciones="tipos"
              etiqueta="Tipo padre (opcional)"
              :opcion-etiqueta="etiquetaTipo"
            />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn
              color="primary"
              unelevated
              type="submit"
              label="Crear"
              :loading="guardando"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>

    <q-dialog v-model="ajusteAbierto" persistent>
      <q-card style="width: min(520px, 94vw)">
        <q-card-section class="text-h6">Ajuste de existencias</q-card-section>
        <q-form @submit.prevent="guardarAjuste">
          <q-card-section class="q-gutter-md">
            <q-select
              v-model="ajuste.almacenId"
              :options="almacenesAjuste"
              emit-value
              map-options
              outlined
              dense
              label="Almacén"
              option-value="almacenId"
              option-label="almacen"
              :rules="[requerido('Seleccione el almacén')]"
            />
            <q-select
              v-model="ajuste.loteId"
              :options="lotesAjuste"
              emit-value
              map-options
              outlined
              dense
              label="Lote"
              option-value="loteId"
              :option-label="l => `${l.numLote} · ${cantidad(l.cantidad)}`"
              :rules="[requerido('Seleccione el lote')]"
            />
            <q-select
              v-model="ajuste.sentido"
              :options="sentidos"
              emit-value
              map-options
              outlined
              dense
              label="Tipo de ajuste"
            />
            <q-input
              v-model.number="ajuste.cantidad"
              type="number"
              min="0.0001"
              step="0.0001"
              outlined
              dense
              label="Cantidad"
              :rules="[numeroPositivo('La cantidad debe ser mayor que cero')]"
            />
            <q-input
              v-model="ajuste.motivo"
              outlined
              dense
              autogrow
              label="Motivo"
              :rules="[requerido('Indique el motivo')]"
            />
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Cancelar" v-close-popup />
            <q-btn
              color="primary"
              unelevated
              type="submit"
              label="Registrar ajuste"
              :loading="guardando"
            />
          </q-card-actions>
        </q-form>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import EncabezadoPagina from '@/components/EncabezadoPagina.vue'
import SelectorBusqueda from '@/components/SelectorBusqueda.vue'
import { useListado } from '@/composables/useListado'
import { usePermiso } from '@/composables/usePermiso'
import { api, listarTodo } from '@/services/api'
import { avisarExito } from '@/utils/dialogo'
import {
  cantidad,
  fecha,
  moneda,
  numeroPositivo,
  requerido
} from '@/utils/formato'

const { permitido } = usePermiso()
const puedeProductos = computed(() => permitido('inventario', 'escritura'))
const puedeAjustar = computed(() => permitido('ajustes', 'escritura'))

const filtros = reactive({
  buscar: '',
  tipoId: null,
  disponibilidad: 'todos',
  almacenId: null
})
const disponibilidades = [
  { value: 'todos', label: 'Todos' },
  { value: 'con', label: 'Con existencia' },
  { value: 'sin', label: 'Sin existencia' }
]
const sentidos = [
  { value: 'entrada', label: 'Sobrante (entrada)' },
  { value: 'salida', label: 'Faltante (salida)' }
]

const { filas, cargando, error, paginacion, cargar, reiniciar } = useListado(
  '/inventario',
  () => ({ ...filtros })
)

const columnas = [
  { name: 'nombre', label: 'Producto', field: 'nombre', align: 'left' },
  {
    name: 'tipo',
    label: 'Tipo',
    field: fila => fila.tipo?.nombre || '—',
    align: 'left'
  },
  {
    name: 'unidad',
    label: 'Unidad',
    field: fila => fila.unidad?.abrev || fila.unidad?.nombre || '—',
    align: 'left'
  },
  {
    name: 'existenciaTotal',
    label: 'Existencia',
    field: 'existenciaTotal',
    align: 'right'
  },
  {
    name: 'costoPromedio',
    label: 'Costo promedio',
    field: 'costoPromedio',
    align: 'right'
  },
  { name: 'acciones', label: '', field: 'acciones', align: 'right' }
]

const tipos = ref([])
const unidades = ref([])
const almacenes = ref([])
const detalle = ref(null)
const detalleAbierto = ref(false)
const cargandoDetalle = ref(false)
const productoAbierto = ref(false)
const productoId = ref(null)
const producto = reactive({
  nombre: '',
  descripcion: '',
  unidadMedidaId: null,
  tipoId: null
})
const unidadAbierta = ref(false)
const unidad = reactive({ nombre: '', abrev: '' })
const tipoAbierto = ref(false)
const tipo = reactive({ nombre: '', padreId: null })
const ajusteAbierto = ref(false)
const ajuste = reactive({
  almacenId: null,
  loteId: null,
  sentido: 'entrada',
  cantidad: 1,
  motivo: ''
})
const guardando = ref(false)

function etiquetaTipo(item) {
  if (!item) return ''
  const padre = tipos.value.find(tipoItem => tipoItem.id === item.padreId)
  return padre ? `${padre.nombre} / ${item.nombre}` : item.nombre
}

const almacenesAjuste = computed(() => detalle.value?.porAlmacen || [])
const lotesAjuste = computed(() =>
  (detalle.value?.lotes || []).filter(
    lote => lote.almacenId === ajuste.almacenId
  )
)

async function cargarCatalogos() {
  try {
    const [tiposData, unidadesData, almacenesData] = await Promise.all([
      listarTodo('/tipos-producto'),
      listarTodo('/unidades-medida'),
      listarTodo('/almacenes')
    ])
    tipos.value = tiposData
    unidades.value = unidadesData
    almacenes.value = almacenesData.filter(item => item.estado !== false)
  } catch {
    // Cada selector queda vacío y el listado muestra su propio error.
  }
}

async function verDetalle(fila) {
  detalleAbierto.value = true
  cargandoDetalle.value = true
  detalle.value = null
  try {
    const { data } = await api.get(`/inventario/${fila.productoId}`)
    detalle.value = data
  } catch {
    detalleAbierto.value = false
  } finally {
    cargandoDetalle.value = false
  }
}

async function abrirProducto(fila) {
  productoId.value = fila?.productoId || null
  producto.nombre = fila?.nombre || ''
  producto.descripcion = fila?.descripcion || ''
  producto.unidadMedidaId = null
  producto.tipoId = fila?.tipo?.id || null
  productoAbierto.value = true
  if (productoId.value) {
    try {
      const { data } = await api.get(`/productos/${productoId.value}`)
      producto.nombre = data.nombre
      producto.descripcion = data.descripcion || ''
      producto.unidadMedidaId = data.unidadMedidaId
      producto.tipoId = data.tipoId
    } catch {
      productoAbierto.value = false
    }
  }
}

async function guardarProducto() {
  guardando.value = true
  const cuerpo = {
    nombre: producto.nombre.trim(),
    descripcion: producto.descripcion?.trim() || null,
    unidadMedidaId: producto.unidadMedidaId,
    tipoId: producto.tipoId
  }
  try {
    if (productoId.value)
      await api.put(`/productos/${productoId.value}`, cuerpo)
    else await api.post('/productos', cuerpo)
    avisarExito(
      productoId.value ? 'Producto actualizado.' : 'Producto registrado.'
    )
    productoAbierto.value = false
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function crearUnidad() {
  guardando.value = true
  try {
    const { data } = await api.post('/unidades-medida', {
      nombre: unidad.nombre.trim(),
      abrev: unidad.abrev.trim()
    })
    unidades.value = await listarTodo('/unidades-medida')
    producto.unidadMedidaId = data?.id || null
    unidad.nombre = ''
    unidad.abrev = ''
    unidadAbierta.value = false
    avisarExito('Unidad creada.')
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

async function crearTipo() {
  guardando.value = true
  try {
    const cuerpo = { nombre: tipo.nombre.trim() }
    if (tipo.padreId) cuerpo.padreId = tipo.padreId
    const { data } = await api.post('/tipos-producto', cuerpo)
    tipos.value = await listarTodo('/tipos-producto')
    producto.tipoId = data?.id || null
    tipo.nombre = ''
    tipo.padreId = null
    tipoAbierto.value = false
    avisarExito('Tipo creado.')
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

function abrirAjuste() {
  ajuste.almacenId = almacenesAjuste.value[0]?.almacenId || null
  ajuste.loteId = null
  ajuste.sentido = 'entrada'
  ajuste.cantidad = 1
  ajuste.motivo = ''
  ajusteAbierto.value = true
}

async function guardarAjuste() {
  const signo = ajuste.sentido === 'salida' ? -1 : 1
  guardando.value = true
  try {
    await api.post('/ajustes', {
      almacenId: ajuste.almacenId,
      loteId: ajuste.loteId,
      cantidad: signo * Number(ajuste.cantidad),
      motivo: ajuste.motivo.trim()
    })
    avisarExito('Ajuste registrado.')
    ajusteAbierto.value = false
    if (detalle.value?.productoId) {
      const { data } = await api.get(`/inventario/${detalle.value.productoId}`)
      detalle.value = data
    }
    await reiniciar()
  } catch {
    // El cliente HTTP ya mostró el error.
  } finally {
    guardando.value = false
  }
}

onMounted(async () => {
  await cargarCatalogos()
  await cargar()
})
</script>
