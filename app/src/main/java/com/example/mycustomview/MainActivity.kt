package com.example.mycustomview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mycustomview.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    var isFirstPlayer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ticTacToeFieldView.ticTacToeField = TicTacToeField(10, 10)
        binding.ticTacToeFieldView.actionListener = { row, column, field ->
            val cell = field.getCell(row, column)
            if(cell == Cell.EMPTY) {
                if(isFirstPlayer) {
                    field.setSell(row, column, Cell.PLAYER_1)
                } else {
                    field.setSell(row, column, Cell.PLAYER_2)
                }
                isFirstPlayer = !isFirstPlayer
            }
        }


    }


}