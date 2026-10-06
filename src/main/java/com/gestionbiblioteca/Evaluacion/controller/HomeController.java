package com.gestionbiblioteca.Evaluacion.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Punto de entrada mínimo para Thymeleaf (sin frontend por ahora). */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("titulo", "Gestor de Biblioteca");
        return "index";
    }
}
