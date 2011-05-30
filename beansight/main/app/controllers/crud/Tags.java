package controllers.crud;
 
import play.mvc.With;
import controllers.CRUD;
import controllers.Check;
import controllers.Secure;

@Check("admin")
@With(Secure.class)
public class Tags extends CRUD {
}
