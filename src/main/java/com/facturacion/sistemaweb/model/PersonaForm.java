package com.facturacion.sistemaweb.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PersonaForm {

	@NotBlank(message = "La cédula es obligatoria.")
	@Pattern(regexp = "[0-9]{10}", message = "La cédula debe contener exactamente 10 dígitos numéricos.")
	private String cedulaPersona;

	@NotBlank(message = "Los nombres son obligatorios.")
	@Size(max = 100, message = "Los nombres admiten hasta 100 caracteres.")
	private String nombresPersona;

	@NotBlank(message = "Los apellidos son obligatorios.")
	@Size(max = 100, message = "Los apellidos admiten hasta 100 caracteres.")
	private String apellidosPersona;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fechaNacimientoPersona;

	public String getCedulaPersona() {
		return cedulaPersona;
	}

	public void setCedulaPersona(String cedulaPersona) {
		this.cedulaPersona = cedulaPersona;
	}

	public String getNombresPersona() {
		return nombresPersona;
	}

	public void setNombresPersona(String nombresPersona) {
		this.nombresPersona = nombresPersona;
	}

	public String getApellidosPersona() {
		return apellidosPersona;
	}

	public void setApellidosPersona(String apellidosPersona) {
		this.apellidosPersona = apellidosPersona;
	}

	public LocalDate getFechaNacimientoPersona() {
		return fechaNacimientoPersona;
	}

	public void setFechaNacimientoPersona(LocalDate fechaNacimientoPersona) {
		this.fechaNacimientoPersona = fechaNacimientoPersona;
	}
}