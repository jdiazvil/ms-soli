CREATE TABLE IF NOT EXISTS estados (
  id_estado      BIGSERIAL PRIMARY KEY,
  nombre         VARCHAR(50)  NOT NULL UNIQUE,
  descripcion    VARCHAR(255)
);

-- -------------------------
-- Tabla: tipo_prestamo
-- -------------------------
CREATE TABLE IF NOT EXISTS tipo_prestamo (
  id_tipo_prestamo     BIGSERIAL PRIMARY KEY,
  nombre               VARCHAR(80)   NOT NULL UNIQUE,
  monto_minimo         NUMERIC(15,2) NOT NULL CHECK (monto_minimo >= 0),
  monto_maximo         NUMERIC(15,2) NOT NULL CHECK (monto_maximo >= monto_minimo),
  -- tasa_interes expresada en %
  tasa_interes         NUMERIC(5,2)  NOT NULL CHECK (tasa_interes > 0 AND tasa_interes <= 100),
  validacion_automatica BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS solicitud (
  id_solicitud      BIGSERIAL PRIMARY KEY,
  monto             NUMERIC(15,2) NOT NULL CHECK (monto > 0),
  plazo             INTEGER       NOT NULL CHECK (plazo > 0),
  email             VARCHAR(120)  NOT NULL,
  id_estado         BIGINT        NOT NULL,
  id_tipo_prestamo  BIGINT        NOT NULL,

  CONSTRAINT fk_solicitud_estado
    FOREIGN KEY (id_estado) REFERENCES estados (id_estado)
    ON UPDATE RESTRICT ON DELETE RESTRICT,

  CONSTRAINT fk_solicitud_tipoprestamo
    FOREIGN KEY (id_tipo_prestamo) REFERENCES tipo_prestamo (id_tipo_prestamo)
    ON UPDATE RESTRICT ON DELETE RESTRICT
);

-- Índices
CREATE INDEX idx_solicitud_email        ON solicitud (email);
CREATE INDEX idx_solicitud_estado       ON solicitud (id_estado);
CREATE INDEX idx_solicitud_tipoprestamo ON solicitud (id_tipo_prestamo);

-- Estados base --
INSERT INTO estados (nombre, descripcion) VALUES
  ('PENDIENTE',   'Pendiente de revisión'),
  ('PREAPROBADO', 'Superó validación automática; pendiente revisión final'),
  ('APROBADO',    'Aprobado por analista'),
  ('RECHAZADO',   'Rechazado por validación o por analista')
ON CONFLICT (nombre) DO NOTHING;

-- Tipos de préstamo
INSERT INTO tipo_prestamo (nombre, monto_minimo, monto_maximo, tasa_interes, validacion_automatica) VALUES
  ('Consumo',     500,     5000,   24.99, TRUE),
  ('Vehicular',   5000,    80000,  13.50, FALSE),
  ('Hipotecario', 20000,   500000, 9.95,  FALSE)
ON CONFLICT (nombre) DO NOTHING;

