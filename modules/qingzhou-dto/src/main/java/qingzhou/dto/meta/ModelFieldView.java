package qingzhou.dto.meta;

import qingzhou.api.ChartType;
import qingzhou.api.FieldType;
import qingzhou.api.InputType;

public class ModelFieldView extends Base {
    public InputType input_type = InputType.text;

    public FieldType field_type = FieldType.form;

    public String display = "";

    public String ref_model = "";

    public String link_to = "";

    public String default_value;

    public String[] options = new String[0];

    public String separator = ",";

    public boolean id;

    public boolean readonly;

    public boolean required;

    public long min_value = Long.MIN_VALUE;

    public long max_value = Long.MAX_VALUE;

    public int min_length = -1;

    public int max_length = Integer.MAX_VALUE;

    public boolean host;

    public boolean port;

    public boolean email;

    public boolean file;

    public String pattern = "";

    public boolean add = true;

    public boolean update = true;

    public boolean show = true;

    public boolean list;

    public boolean search;

    public boolean numeric;

    public ChartType chart_type = ChartType.line;

    public String group = "";

    public String chart_title = "";

    public String[] color = new String[0];

    public int width_percent = -1;

    public String[] name = new String[0];

    public String[] info = new String[0];
}
