package br.com.caelum.leilao.infra.relogio;

import java.util.Calendar;

public class RelogioDoSistema implements Relogio {
    @Override
    public Calendar hoje() {
        return Calendar.getInstance();
    }
}
