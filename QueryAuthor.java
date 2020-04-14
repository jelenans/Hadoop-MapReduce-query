package org.hwone;

import java.util.Scanner;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import org.json.*;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class QueryAuthor {

public static class BookMapper 
       extends Mapper<Object, Text, Text, Text>{

    private Text word = new Text();
      
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {

     Configuration conf = context.getConfiguration();
     String param = conf.get("param");

     Scanner scn=new Scanner(value.toString());
     ArrayList<JSONObject> json=new ArrayList<JSONObject>();
     String author="";
     String book="";
  
     while(scn.hasNext()){
      try{
          JSONObject obj= (JSONObject) new JSONParser().parse(scn.nextLine());
          json.add(obj);
      }catch(ParseException e){
            e.printStackTrace();
      }
     }

   
// {"author": "Tobias Wells", "books": [{"book":"A die in the country"},{"book": "Dinky died"}]}
       for(JSONObject obj : json){
            author=(String)obj.get("author");      

            if(author.equals(param))
            {   
              book= (String)obj.get("book");
              context.write(new Text(author), new Text(book));
            }

      }

   

    }
  }

  public static class BookCombiner extends Reducer<Text, Text, Text, Text>{

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {



    JSONObject jo = new JSONObject();
    JSONArray ja = new JSONArray();
  

 //{"author": "Tobias Wells", "books": [{"book":"A die in the country"},{"book": "Dinky died"}]}

    for(Text val : values){
          JSONObject joBook = new JSONObject();
          joBook.put("book" , val.toString());
          ja.put(joBook);
     }

       
      context.write(key, new Text(ja.toString()));
      
    
   }
  }
  

  public static class BookReducer extends Reducer<Text,Text,Text,NullWritable> {
  

    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

    JSONObject jo = new JSONObject();
    
    Configuration conf = context.getConfiguration();
    String param = conf.get("param");

    String author= key.toString();  


      for(Text val : values){
          JSONObject joBook = new JSONObject();
          if(!(val.toString()).contains("book"))
          {
           joBook.put("book" , val.toString());
           jo.put("books", joBook);
          } else
          {
           jo.put("books", val.toString());
          }
      

        jo.put("author", key.toString());
      
       
        String result= jo.toString();
        result= result.replace("\\","");   
        context.write(new Text(result), NullWritable.get());
    }

       
      
    
   }
  }

  public static void main(String[] args) throws Exception {
     Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args)
        .getRemainingArgs();

    if (otherArgs.length < 3) {
      System.err.println("Usage: QueryAuthor <in> <out> <author>");
      System.exit(2);
    }
	
    String param=null;
    for(int i=2;i<otherArgs.length;i++)
    {
       if(param!=null)
       {
	  param+= " "+ otherArgs[i];
       }
       else
       {
	   param= otherArgs[i];
       }
    }
   
    conf.set("param", param);
    Job job = new Job(conf, "QueryAuthor");
    job.setJarByClass(QueryAuthor.class);
    job.setMapperClass(BookMapper.class);
    job.setCombinerClass(BookCombiner.class);
    job.setReducerClass(BookReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);
    job.setOutputValueClass(NullWritable.class);

    FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
    FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));


    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
