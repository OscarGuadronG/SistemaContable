<template>
  <q-page class="pagina flex flex-center q-pa-lg">
    <q-card flat bordered class="q-pa-lg text-center" style="max-width: 460px">
      <q-icon name="lock" size="56px" color="warning" />
      <div class="text-h5 text-weight-bold q-mt-md">Sin permiso</div>
      <p class="text-body1 text-grey-8">
        Su rol no puede abrir esta pantalla. Si cree que se trata de un error,
        pida al administrador que revise su perfil.
      </p>
      <q-btn color="primary" unelevated label="Volver al inicio" to="/" />
    </q-card>
  </q-page>
</template>
