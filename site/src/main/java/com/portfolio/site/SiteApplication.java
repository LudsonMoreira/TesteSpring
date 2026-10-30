package com.portfolio.site;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class SiteApplication implements CommandLineRunner {

    private static final String BANCO = "data/votacao.db";
    private static final String GRAFICO = "data/grafico.png";

    private static final List<String> LINGUAGENS = List.of(
        "Java", "Python", "JavaScript", "TypeScript",
        "PHP", "C#", "C++", "Go", "Rust", "SQL"
    );

    public static void main(String[] args) {
        SpringApplication.run(SiteApplication.class, args);
    }

    // Cria o banco e gera o gráfico inicial.
    @Override
    public void run(String... args) throws Exception {

        Files.createDirectories(Path.of("data"));

        try (
            Connection conexao = DriverManager.getConnection(
                "jdbc:sqlite:" + BANCO
            );
            Statement sql = conexao.createStatement()
        ) {
            sql.execute("""
                CREATE TABLE IF NOT EXISTS votos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    linguagem TEXT NOT NULL,
                    criado_em TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }

        atualizarGrafico();
    }

    // Recebe e registra um voto.
    @PostMapping("/api/votar")
    public ResponseEntity<Map<String, String>> votar(
        @RequestBody Map<String, String> dados
    ) throws SQLException {

        String linguagem = dados == null
            ? null
            : dados.get("linguagem");

        if (!LINGUAGENS.contains(linguagem)) {
            return ResponseEntity.badRequest().body(
                Map.of("erro", "Selecione uma linguagem válida.")
            );
        }

        try (
            Connection conexao = DriverManager.getConnection(
                "jdbc:sqlite:" + BANCO
            );
            PreparedStatement sql = conexao.prepareStatement(
                "INSERT INTO votos (linguagem) VALUES (?)"
            )
        ) {
            sql.setString(1, linguagem);
            sql.executeUpdate();
        }

        atualizarGrafico();

        return ResponseEntity.ok(
            Map.of("mensagem", "Voto registrado com sucesso!")
        );
    }

    // Consulta a quantidade de votos por linguagem.
    @GetMapping("/api/resultados")
    public List<Map<String, Object>> resultados()
        throws SQLException {

        List<Map<String, Object>> dados = new ArrayList<>();

        try (
            Connection conexao = DriverManager.getConnection(
                "jdbc:sqlite:" + BANCO
            );
            Statement sql = conexao.createStatement();
            ResultSet resultado = sql.executeQuery("""
                SELECT linguagem, COUNT(*) AS total
                FROM votos
                GROUP BY linguagem
                ORDER BY total DESC
            """)
        ) {
            while (resultado.next()) {
                dados.add(Map.<String, Object>of(
                    "linguagem",
                    resultado.getString("linguagem"),
                    "votos",
                    resultado.getInt("total")
                ));
            }
        }

        return dados;
    }

    // Entrega ao navegador a imagem gerada pelo Python.
    @GetMapping(
        value = "/api/grafico",
        produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> grafico() throws Exception {

        Path arquivo = Path.of(GRAFICO);

        if (!Files.exists(arquivo)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.noStore())
            .body(Files.readAllBytes(arquivo));
    }

    // Java executa o programa Python.
    private synchronized void atualizarGrafico() {

        try {
            Process processo = new ProcessBuilder(
                ".venv/bin/python",
                "python/gerar_grafico.py",
                BANCO,
                GRAFICO
            ).redirectErrorStream(true).start();

            String saida = new String(
                processo.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );

            int codigo = processo.waitFor();

            if (codigo != 0) {
                System.err.println("Erro no Python: " + saida);
            }

        } catch (Exception erro) {
            System.err.println("Não foi possível gerar o gráfico.");
            erro.printStackTrace();
        }
    }
}

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication
// public class SiteApplication {

// 	public static void main(String[] args) {
// 		SpringApplication.run(SiteApplication.class, args);
// 	}

// }
