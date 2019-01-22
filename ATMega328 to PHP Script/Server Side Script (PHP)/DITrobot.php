<html>
<head><title>DIT robot control interface</title>
<body>


    <table border="1">
    <tr>
    <td>ID</td>
    <td>Battery Power (%)</td>
    <td>Temperature (C)</td>
    <td>Pressure (Pa)</td>
    <td>Altitude (m)</td>
    <td>Date/Time</td>
    </tr>



    <?php

    
    //****************Appends new data values to a text file. ********

     $fileContent = "You have ". $x ."and ". $y;
     $fileStatus = file_put_contents('myFile.txt' ,$fileContent, FILE_APPEND);
     if($fileStatus != false)
     {
          echo "Success: data was written to file";
     }



    //****************Prints out all the values from the database. ********

    echo $x . "<br>";
    echo $y . "<br>";
    

    $servername = "mysql11.000webhost.com";
    $username = "a2467249_tyrone";
    $password = "Euiyv^ue8s";
    $dbname = "a2467249_robot";
     
    // Create connection
    $conn = new mysqli($servername, $username, $password, $dbname);
    // Check connection
    if ($conn->connect_error) {
        die("Connection failed: " . $conn->connect_error);
    }
     
     //Check if there are given parameters in the URL.
	if(isset($_GET['param1'])&&isset($_GET['param2'])&&isset($_GET['param3'])&&isset($_GET['param4']))
	{
		$batterypower = $_GET['param1']; //param1 is stored in $batterypower.
		$temperature = $_GET['param2']; //param2 is stored in $temperature.
		$pressure = $_GET['param3']; //param3 is stored in $pressure.
		$altitude = $_GET['param4']; //param4 is stored in $altitude.
		//Checks if the values of temp, pressure and altitude are not zero.
		if(!empty($temperature)&&!empty($temperature)&&!empty($pressure)&&!empty($altitude))
		{
			//Insert the value of temp, pressure and altitude into a table called ditrobot
			$sql = "INSERT INTO ditrobot (BatteryPower, Temperature, Pressure, Altitude)
			VALUES ('$batterypower', '$temperature', '$pressure', '$altitude')";
			//Pointer is incremented to next row if there is new data that been inserted.
			if ($conn->query($sql) === TRUE) 
			{
				echo "New record created successfully". "<br>";
			} else {
				echo "Error: " . $sql . "<br>" . $conn->error;
			}
		}
	
	}


     
    
    //Prints out all value values from the database. 
    $sql = "SELECT * FROM ditrobot";
    $result = $conn->query($sql);
     
    if ($result->num_rows > 0) {
        // output data of each row

        while($row = $result->fetch_assoc()) {
            echo "<tr>";
            echo "<td>". $row["id"]. "</td>".PHP_EOL;
            echo "<td>". $row["BatteryPower"]. "</td>".PHP_EOL;
            echo "<td>". $row["Temperature"]. "</td>".PHP_EOL;
            echo "<td>". $row["Pressure"]. "</td>".PHP_EOL;
            echo "<td>". $row["Altitude"]. "</td>".PHP_EOL;
            echo "<td>". $row["Time"]. "</td>".PHP_EOL;
            echo "</tr>";
        }

    } else {
        echo "0 results";
    }
  ?>
  
  </table>

</body>
</html>	


http://www.tixgy.com/DITrobot.php/?param1=1&param2=2&param3=3&param4=4
										