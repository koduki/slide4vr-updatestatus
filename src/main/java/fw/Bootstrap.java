/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fw;

import java.io.IOException;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

/**
 *
 * @author koduki
 */
@ApplicationScoped
public class Bootstrap {


    public void handle(@Observes @Initialized(ApplicationScoped.class) Object event) throws IOException {
    }
}
