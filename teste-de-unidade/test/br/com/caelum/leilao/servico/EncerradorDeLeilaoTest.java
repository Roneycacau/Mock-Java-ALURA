package br.com.caelum.leilao.servico;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.email.EnviadorDeEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EncerradorDeLeilaoTest {

    private Calendar data;
    private Leilao leilao1;
    private Leilao leilao2;
    private EnviadorDeEmail carteiroMock;
    private RepositorioDeLeiloes daoMock;
    private EncerradorDeLeilao encerrador;


    @BeforeEach
    public void setup(){

        data = Calendar.getInstance();
        leilao1 = new CriadorDeLeilao().para("TV").naData(data).constroi();
        leilao2 = new CriadorDeLeilao().para("Geladeira").naData(data).constroi();
        daoMock = mock(RepositorioDeLeiloes.class);
        carteiroMock = mock(EnviadorDeEmail.class);
        encerrador = new EncerradorDeLeilao(daoMock, carteiroMock);

    }

    @Test
    public void deveEncerrarLeiloesQueComecaramUmaSemanaAntes() {
        data.set(1999, 1, 20);

        List<Leilao> leiloesAntigos = Arrays.asList(leilao1, leilao2);

        when(daoMock.correntes()).thenReturn(leiloesAntigos);

        encerrador.encerra();

        assertEquals(2, encerrador.getTotalEncerrados());
        assertTrue(leilao1.isEncerrado());
        assertTrue(leilao2.isEncerrado());

    }

    @Test
    public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras(){
        data.add(Calendar.DAY_OF_MONTH, - 1);

        when(daoMock.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        assertEquals(0, encerrador.getTotalEncerrados());
        assertFalse(leilao1.isEncerrado());
        assertFalse(leilao2.isEncerrado());
        verify(daoMock, never()).atualiza(leilao1);
        verify(daoMock, never()).atualiza(leilao2);
    }

    @Test
    public void naoDeveEncerrarLeiloesCasoNaoHajaNenhum() {

        when(daoMock.correntes()).thenReturn(new ArrayList<Leilao>());

        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoMock, carteiroMock);
        encerrador.encerra();

        assertEquals(0, encerrador.getTotalEncerrados());
    }

    @Test
    public void deveAtualizarLeiloesEncerrados(){

        data.set(1999,1,20);

        when(daoMock.correntes()).thenReturn(Arrays.asList((leilao1)));
        encerrador.encerra();

        verify(daoMock, atMost(1)).atualiza(leilao1);
    }

    @Test
    public void deveEnviarEmailAposPersistirLeilaoEncerrado() {
        data.set(1999, 1, 20);

        when(daoMock.correntes()).thenReturn(Arrays.asList(leilao1));
        encerrador.encerra();

        InOrder inOrder = inOrder(daoMock, carteiroMock);
        inOrder.verify(daoMock, times(1)).atualiza(leilao1);
        inOrder.verify(carteiroMock, times(1)).envia(leilao1);
    }

    @Test
    public void deveContinuarAExecutarMesmoQuandoDaoFalha(){
        data.set(1999,1,20);

        when(daoMock.correntes()).thenReturn(Arrays.asList(leilao1,leilao2));

        doThrow(new RuntimeException()).when(daoMock).atualiza(leilao1);

        encerrador.encerra();

        verify(daoMock).atualiza(leilao2);
        verify(carteiroMock).envia(leilao2);

        verify(carteiroMock, times(0)).envia(leilao1);
    }
}
