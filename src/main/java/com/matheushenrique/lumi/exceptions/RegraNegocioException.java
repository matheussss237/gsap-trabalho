package com.matheushenrique.lumi.exceptions;

// erro de validação ou de regra (campo inválido, senha incorreta, API fora do ar...)
// a mensagem já é escrita para o usuário ler na tela
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }

    public RegraNegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
